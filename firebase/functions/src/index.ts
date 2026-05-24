import { onSchedule, ScheduledEvent } from "firebase-functions/v2/scheduler";
import { logger } from "firebase-functions";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp, FieldValue } from "firebase-admin/firestore";
import * as cheerio from "cheerio";

initializeApp();
const db = getFirestore();

// ---------- types ----------

interface Product {
  url: string;
  sku: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  images: string[];
  lowPrice: number | null;
  highPrice: number | null;
  priceCurrency: string | null;
  source: string;
  scrapedAt: FirebaseFirestore.Timestamp;
}

// ---------- low-level helpers ----------

const USER_AGENT = "Mozilla/5.0 (compatible; MamacitaMKBot/1.0; +https://github.com/yourname)";
const LOC_RE = /<loc>([^<]+)<\/loc>/g;

async function fetchText(url: string): Promise<string> {
  const res = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!res.ok) throw new Error(`GET ${url} -> HTTP ${res.status}`);
  return res.text();
}

async function mapWithConcurrency<T, R>(
  items: T[],
  concurrency: number,
  fn: (item: T, index: number) => Promise<R>,
): Promise<PromiseSettledResult<R>[]> {
  const results: PromiseSettledResult<R>[] = new Array(items.length);
  let cursor = 0;
  const workers = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (true) {
      const i = cursor++;
      if (i >= items.length) return;
      try {
        results[i] = { status: "fulfilled", value: await fn(items[i], i) };
      } catch (reason) {
        results[i] = { status: "rejected", reason };
      }
    }
  });
  await Promise.all(workers);
  return results;
}

// ---------- babycenter.mk scraper ----------

const BC_SITEMAP_INDEX = "https://www.babycenter.mk/sitemap.xml";
const BC_SOURCE = "babycenter.mk";

async function bcDiscoverProductUrls(): Promise<string[]> {
  const indexXml = await fetchText(BC_SITEMAP_INDEX);
  const childSitemaps = [...indexXml.matchAll(LOC_RE)]
    .map((m) => m[1].trim())
    .filter((u) => u.toLowerCase().includes("product"));

  const urls: string[] = [];
  for (const sitemap of childSitemaps) {
    const xml = await fetchText(sitemap);
    for (const m of xml.matchAll(LOC_RE)) {
      urls.push(m[1].trim());
    }
  }
  return Array.from(new Set(urls));
}

function bcParseProductPage(html: string, url: string): Product | null {
  const $ = cheerio.load(html);
  const blocks = $("script[type='application/ld+json']")
    .map((_, el) => $(el).text())
    .get();

  for (const raw of blocks) {
    let data: any;
    try {
      data = JSON.parse(raw);
    } catch {
      continue;
    }
    if (data?.["@type"] !== "Product") continue;

    const images: string[] = Array.isArray(data.image)
      ? data.image.filter((x: unknown): x is string => typeof x === "string")
      : typeof data.image === "string"
        ? [data.image]
        : [];

    const brand =
      (typeof data.brand === "object" && typeof data.brand?.name === "string"
        ? data.brand.name
        : null) ??
      (typeof data.brand === "string" ? data.brand : null);

    const offers = data.offers ?? {};
    const lowPrice =
      toNumberOrNull(offers.lowPrice) ?? toNumberOrNull(offers.price);
    const highPrice = toNumberOrNull(offers.highPrice);

    return {
      url,
      sku: typeof data.sku === "string" ? data.sku : null,
      name: typeof data.name === "string" ? data.name : "",
      brand,
      description: typeof data.description === "string" ? data.description : null,
      images,
      lowPrice,
      highPrice,
      priceCurrency: typeof offers.priceCurrency === "string" ? offers.priceCurrency : null,
      source: BC_SOURCE,
      scrapedAt: Timestamp.now(),
    };
  }
  return null;
}

function toNumberOrNull(v: unknown): number | null {
  if (v == null) return null;
  const n = typeof v === "number" ? v : Number(v);
  return Number.isFinite(n) ? n : null;
}

async function bcScrapeProduct(url: string): Promise<Product | null> {
  const html = await fetchText(url);
  return bcParseProductPage(html, url);
}

// ---------- Firestore writes ----------

/** Stable doc ID: prefer the numeric sku, fall back to a URL-safe slug. */
function docIdFor(product: Product): string {
  if (product.sku && /^\w+$/.test(product.sku)) return `${product.source}__${product.sku}`;
  return `${product.source}__${Buffer.from(product.url).toString("base64url")}`;
}

async function upsertProducts(products: Product[]): Promise<void> {
  // Firestore batched writes cap at 500 ops; chunk accordingly.
  const CHUNK = 400;
  for (let i = 0; i < products.length; i += CHUNK) {
    const batch = db.batch();
    for (const p of products.slice(i, i + CHUNK)) {
      const ref = db.collection("products").doc(docIdFor(p));
      batch.set(ref, p, { merge: true });
    }
    await batch.commit();
  }
}

async function recordRun(source: string, stats: Record<string, unknown>): Promise<void> {
  await db.collection("scrape_runs").add({
    source,
    finishedAt: FieldValue.serverTimestamp(),
    ...stats,
  });
}

// ---------- scheduled entry point ----------

export const scrapeBabyCenter = onSchedule(
  {
    schedule: "every 6 hours",
    timeZone: "Europe/Skopje",
    timeoutSeconds: 540,
    memory: "512MiB",
    region: "europe-west1",
    retryCount: 0,
  },
  async (_event: ScheduledEvent) => {
    const startedAt = Date.now();
    logger.info("babycenter.mk scrape: discovering product URLs");

    const urls = await bcDiscoverProductUrls();
    logger.info(`babycenter.mk scrape: discovered ${urls.length} URLs`);

    const buffer: Product[] = [];
    let ok = 0;
    let failed = 0;
    let skipped = 0;
    const FLUSH_EVERY = 100;
    const CONCURRENCY = 8;

    const flush = async () => {
      if (buffer.length === 0) return;
      await upsertProducts(buffer);
      logger.info(`flushed ${buffer.length} products (running totals: ok=${ok} skipped=${skipped} failed=${failed})`);
      buffer.length = 0;
    };

    const results = await mapWithConcurrency(urls, CONCURRENCY, async (url) => {
      const product = await bcScrapeProduct(url);
      if (!product) return null;
      buffer.push(product);
      if (buffer.length >= FLUSH_EVERY) await flush();
      return product;
    });

    for (const r of results) {
      if (r.status === "fulfilled") {
        if (r.value) ok++;
        else skipped++;
      } else {
        failed++;
      }
    }

    await flush();

    const elapsedMs = Date.now() - startedAt;
    await recordRun(BC_SOURCE, {
      discovered: urls.length,
      ok,
      skipped,
      failed,
      elapsedMs,
    });

    logger.info(`babycenter.mk scrape done in ${elapsedMs}ms — ok=${ok} skipped=${skipped} failed=${failed}`);
  },
);

// ---------- TODO: add scrapeBebeSupermarket and scrapeLibertaBebeCentar ----------
// Pattern: write a discover + parse pair like the bc* helpers above, then export
// another `onSchedule(...)` with a different `source` tag. Firestore docs are
// keyed by `${source}__${sku-or-hash}` so collections stay merged but per-site
// scrapes don't trample each other.

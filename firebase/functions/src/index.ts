/**
 * Standalone scraper. Runs on GitHub Actions cron (see
 * .github/workflows/scrape.yml) and writes products to Firestore via the
 * Firebase Admin SDK.
 *
 * Auth:
 *   - In CI: reads FIREBASE_SERVICE_ACCOUNT env var (JSON string from a GH secret).
 *   - Locally: falls back to Application Default Credentials
 *     (run `gcloud auth application-default login` first).
 */

import { initializeApp, cert, applicationDefault } from "firebase-admin/app";
import { getFirestore, Timestamp, FieldValue } from "firebase-admin/firestore";
import * as cheerio from "cheerio";

// ---------- bootstrap ----------

function bootstrapFirebase(): void {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (raw) {
    initializeApp({ credential: cert(JSON.parse(raw)) });
    console.log("Firebase initialised from FIREBASE_SERVICE_ACCOUNT env var");
  } else {
    initializeApp({ credential: applicationDefault() });
    console.log("Firebase initialised from Application Default Credentials");
  }
}

bootstrapFirebase();
const db = getFirestore();

// ---------- shared types ----------

interface ProductCategory {
  name: string;
  slug: string;
  url: string;
}

interface Product {
  url: string;
  source: string;
  sku: string | null;
  internalId: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  images: string[];
  categories: ProductCategory[];
  regularPrice: number | null;
  salePrice: number | null;
  priceCurrency: string | null;
  discountPercent: number | null;
  saleValidFrom: string | null;
  saleValidTo: string | null;
  inStock: boolean | null;
  isOnSale: boolean | null;
  isNew: boolean | null;
  isLastPiece: boolean | null;
  isInternetOnly: boolean | null;
  hasClubPrice: boolean | null;
  attributes: Record<string, string>;
  deliveryInfo: string | null;
  scrapedAt: FirebaseFirestore.Timestamp;
}

// ---------- low-level helpers ----------

const USER_AGENT = "Mozilla/5.0 (compatible; MamacitaMKBot/1.0; +https://github.com/LearnWithSimona/MamacitaMK)";
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

function toNumberOrNull(v: unknown): number | null {
  if (v == null) return null;
  const n = typeof v === "number" ? v : Number(v);
  return Number.isFinite(n) ? n : null;
}

function urlToSlug(url: string): string {
  try {
    return new URL(url).pathname.replace(/^\/|\/$/g, "");
  } catch {
    return "";
  }
}

/** "Важи од 21.05.2026 до 28.05.2026" -> { from: "2026-05-21", to: "2026-05-28" } */
function parseSaleValidity(text: string): { from: string | null; to: string | null } {
  if (!text) return { from: null, to: null };
  const m = text.match(/(\d{2})\.(\d{2})\.(\d{4}).*?(\d{2})\.(\d{2})\.(\d{4})/);
  if (!m) return { from: null, to: null };
  return { from: `${m[3]}-${m[2]}-${m[1]}`, to: `${m[6]}-${m[5]}-${m[4]}` };
}

function parseDataLayer(html: string): Record<string, unknown> {
  const m = html.match(/window\.ezdatalayer\s*=\s*({[\s\S]*?});/);
  if (!m) return {};
  try {
    return JSON.parse(m[1]);
  } catch {
    return {};
  }
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

  // 1. JSON-LD: Product + BreadcrumbList
  let productJson: any = null;
  let breadcrumbJson: any = null;
  $("script[type='application/ld+json']").each((_, el) => {
    let data: any;
    try {
      data = JSON.parse($(el).text());
    } catch {
      return;
    }
    if (data?.["@type"] === "Product") productJson = data;
    if (data?.["@type"] === "BreadcrumbList") breadcrumbJson = data;
  });
  if (!productJson) return null;

  // 2. ezdatalayer flags
  const dl = parseDataLayer(html);

  // 3. Categories from breadcrumb (drop the trailing item — that's the product itself)
  const items = (breadcrumbJson?.itemListElement ?? []) as Array<{
    position: number;
    name: string;
    item: string;
  }>;
  const categories: ProductCategory[] = items
    .slice()
    .sort((a, b) => a.position - b.position)
    .slice(0, -1)
    .map((i) => ({ name: i.name, slug: urlToSlug(i.item), url: i.item }));

  // 4. Prices from JSON-LD offers
  const offers = productJson.offers ?? {};
  const high = toNumberOrNull(offers.highPrice);
  const low = toNumberOrNull(offers.lowPrice) ?? toNumberOrNull(offers.price);
  let regularPrice: number | null = null;
  let salePrice: number | null = null;
  if (high != null && low != null && low < high) {
    regularPrice = high;
    salePrice = low;
  } else {
    regularPrice = high ?? low;
  }
  const discountPercent =
    regularPrice != null && salePrice != null
      ? Math.round(((regularPrice - salePrice) / regularPrice) * 100)
      : null;

  // 5. Sale validity dates
  const { from: saleValidFrom, to: saleValidTo } = parseSaleValidity($(".activeTo").text());

  // 6. Internal id (WooCommerce post id) from `class="product type-product product-836787"`
  const productClass = $(".product.type-product").attr("class") || "";
  const internalIdMatch = productClass.match(/product-(\d+)/);
  const internalId = internalIdMatch ? internalIdMatch[1] : null;

  // 7. Status flags from the data layer
  const inStock = typeof dl.item_stock === "boolean" ? dl.item_stock : null;
  const isOnSale = typeof dl.item_action === "boolean" ? dl.item_action : null;
  const isNew = typeof dl.item_new === "boolean" ? dl.item_new : null;
  const isLastPiece = typeof dl.item_last === "boolean" ? dl.item_last : null;
  const isInternetOnly = typeof dl.item_netonly === "number" ? dl.item_netonly === 1 : null;
  const hasClubPrice = typeof dl.item_club === "number" ? dl.item_club > 0 : null;

  // 8. Spec table (skip SKU row since we have it as a top-level field)
  const attributes: Record<string, string> = {};
  $(".shop_attributes tbody tr").each((_, el) => {
    const key = $(el).find("th").text().trim();
    const val = $(el).find("td").text().trim();
    if (key && val && !/sku|шифра/i.test(key)) attributes[key] = val;
  });

  // 9. Delivery info
  const deliveryRaw = $(".itemStockDelivery").text().trim().replace(/\s+/g, " ");
  const deliveryInfo = deliveryRaw.length > 0 ? deliveryRaw : null;

  // 10. Description: prefer meta (clean) over JSON-LD (triple-encoded HTML)
  const metaDesc = $("meta[name='description']").attr("content")?.trim();
  const description =
    metaDesc && metaDesc.length > 0
      ? metaDesc
      : typeof productJson.description === "string"
        ? productJson.description
        : null;

  // 11. Images
  const images: string[] = Array.isArray(productJson.image)
    ? productJson.image.filter((x: unknown): x is string => typeof x === "string")
    : typeof productJson.image === "string"
      ? [productJson.image]
      : [];

  // 12. Brand
  const brand =
    (typeof productJson.brand === "object" && typeof productJson.brand?.name === "string"
      ? productJson.brand.name
      : null) ?? (typeof productJson.brand === "string" ? productJson.brand : null);

  // 13. Currency normalisation: their JSON-LD says "ден", standardise to ISO "MKD"
  const rawCurrency = typeof offers.priceCurrency === "string" ? offers.priceCurrency : null;
  const priceCurrency = rawCurrency === "ден" ? "MKD" : rawCurrency;

  return {
    url,
    source: BC_SOURCE,
    sku: typeof productJson.sku === "string" ? productJson.sku : null,
    internalId,
    name: typeof productJson.name === "string" ? productJson.name : "",
    brand,
    description,
    images,
    categories,
    regularPrice,
    salePrice,
    priceCurrency,
    discountPercent,
    saleValidFrom,
    saleValidTo,
    inStock,
    isOnSale,
    isNew,
    isLastPiece,
    isInternetOnly,
    hasClubPrice,
    attributes,
    deliveryInfo,
    scrapedAt: Timestamp.now(),
  };
}

async function bcScrapeProduct(url: string): Promise<Product | null> {
  const html = await fetchText(url);
  return bcParseProductPage(html, url);
}

// ---------- libertabebecentar.mk scraper ----------
//
// All product data lives in the category listing pages — each `.product-thumb`
// is a schema.org microdata block with name, image, price, currency, availability.
// No per-product detail page fetch needed.

const LB_BASE = "https://www.libertabebecentar.mk";
const LB_SOURCE = "libertabebecentar.mk";
const LB_CATEGORIES: ReadonlyArray<string> = [
  "kolichki",
  "wooden-beds",
  "krevetchinja",
  "postelnina",
  "tekstil",
  "igrachki-2",
  "igrachki",
  "koli-na-akumulator",
  "relaksatori",
];

function lbParseCards(html: string, slug: string): Product[] {
  const $ = cheerio.load(html);
  const out: Product[] = [];

  $("div.product-thumb").each((_, el) => {
    const card = $(el);

    const url = card.find("a[itemprop=url]").first().attr("href")?.trim()
      ?? card.find("h3 a").first().attr("href")?.trim();
    if (!url) return;

    const name = card.find("[itemprop=name]").first().text().trim();
    const image = card.find("img[itemprop=image]").first().attr("src")
      ?? card.find("img.main-img").first().attr("src");
    const description = card.find("[itemprop=description]").first().text().trim() || null;

    const offers = card.find("[itemprop=offers]").first();
    const priceStr = offers.find("meta[itemprop=price]").attr("content") ?? "";
    const regularPrice = toNumberOrNull(priceStr);
    const currency = offers.find("meta[itemprop=priceCurrency]").attr("content") || null;
    const availability = offers.find("[itemprop=availability]").attr("href") ?? "";
    const inStock = availability.includes("InStock");
    const condition = offers.find("meta[itemprop=itemCondition]").attr("content") || null;

    const categoryName = card.find("meta[itemprop=category]").attr("content")?.trim() || slug;

    // OpenCart product id from `onclick="cart.add('984');"` or similar.
    const onclickStr = card.find("[onclick*='cart.add']").first().attr("onclick") ?? "";
    const idMatch = onclickStr.match(/cart\.add\(\s*'?(\d+)'?\s*\)/);
    const internalId = idMatch ? idMatch[1] : null;

    const attributes: Record<string, string> = {};
    if (condition) attributes["condition"] = condition.replace(/^https?:\/\/schema\.org\//, "");

    out.push({
      url,
      source: LB_SOURCE,
      sku: null,
      internalId,
      name,
      brand: null,
      description,
      images: image ? [image] : [],
      categories: [{ name: categoryName, slug, url: `${LB_BASE}/${slug}` }],
      regularPrice,
      salePrice: null,
      priceCurrency: currency,
      discountPercent: null,
      saleValidFrom: null,
      saleValidTo: null,
      inStock,
      isOnSale: null,
      isNew: null,
      isLastPiece: null,
      isInternetOnly: null,
      hasClubPrice: null,
      attributes,
      deliveryInfo: null,
      scrapedAt: Timestamp.now(),
    });
  });

  return out;
}

async function lbScrapeCategory(slug: string): Promise<Product[]> {
  const products: Product[] = [];
  const seen = new Set<string>();
  for (let page = 1; page <= 30; page++) {
    const url = page === 1 ? `${LB_BASE}/${slug}` : `${LB_BASE}/${slug}?page=${page}`;
    const html = await fetchText(url);
    const cards = lbParseCards(html, slug);
    const fresh = cards.filter((c) => !seen.has(c.url));
    if (fresh.length === 0) break;
    for (const p of fresh) {
      seen.add(p.url);
      products.push(p);
    }
  }
  return products;
}

async function scrapeLibertaBebeCentar(): Promise<void> {
  const startedAt = Date.now();
  console.log("libertabebecentar.mk: scraping categories");

  // Dedupe across categories — a product may appear in multiple sections; merge its categories.
  const byUrl = new Map<string, Product>();
  let categoryFailures = 0;

  for (const slug of LB_CATEGORIES) {
    try {
      const products = await lbScrapeCategory(slug);
      console.log(`  ${slug}: ${products.length} products`);
      for (const p of products) {
        const existing = byUrl.get(p.url);
        if (existing) {
          for (const c of p.categories) {
            if (!existing.categories.some((e) => e.slug === c.slug)) {
              existing.categories.push(c);
            }
          }
        } else {
          byUrl.set(p.url, p);
        }
      }
    } catch (err) {
      console.error(`  ${slug}: failed —`, (err as Error).message);
      categoryFailures++;
    }
  }

  const all = Array.from(byUrl.values());
  await upsertProducts(all);

  const elapsedMs = Date.now() - startedAt;
  await recordRun(LB_SOURCE, {
    categoriesScraped: LB_CATEGORIES.length - categoryFailures,
    categoryFailures,
    productsWritten: all.length,
    elapsedMs,
  });
  console.log(`libertabebecentar.mk done in ${elapsedMs}ms — products=${all.length} categoryFailures=${categoryFailures}`);
}

// ---------- Firestore writes ----------

function docIdFor(product: Product): string {
  if (product.sku && /^\w+$/.test(product.sku)) return `${product.source}__${product.sku}`;
  return `${product.source}__${Buffer.from(product.url).toString("base64url")}`;
}

async function upsertProducts(products: Product[]): Promise<void> {
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

// ---------- main ----------

async function scrapeBabyCenter(): Promise<void> {
  const startedAt = Date.now();
  console.log("babycenter.mk: discovering product URLs");
  const urls = await bcDiscoverProductUrls();
  console.log(`babycenter.mk: discovered ${urls.length} URLs`);

  const buffer: Product[] = [];
  let ok = 0;
  let failed = 0;
  let skipped = 0;
  const FLUSH_EVERY = 100;
  const CONCURRENCY = 8;

  const flush = async () => {
    if (buffer.length === 0) return;
    await upsertProducts(buffer);
    console.log(`flushed ${buffer.length} (running: ok=${ok} skipped=${skipped} failed=${failed})`);
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
  await recordRun(BC_SOURCE, { discovered: urls.length, ok, skipped, failed, elapsedMs });
  console.log(`babycenter.mk done in ${elapsedMs}ms — ok=${ok} skipped=${skipped} failed=${failed}`);
}

async function main(): Promise<void> {
  await scrapeBabyCenter();
  await scrapeLibertaBebeCentar();
  // TODO: scrapeBebeSupermarket() can be added here.
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Scrape failed:", err);
    process.exit(1);
  });

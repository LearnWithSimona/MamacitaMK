# MamacitaMK — Firebase backend

Scheduled Cloud Functions that scrape baby-store sites and write products into Firestore. The Android app reads via the Firestore SDK (offline cache included).

## Layout

```
firebase/
├── firebase.json              project config
├── firestore.rules            public read on /products, no client writes
├── firestore.indexes.json
└── functions/
    ├── package.json
    ├── tsconfig.json
    └── src/index.ts           the scraper(s)
```

## One-time setup

```bash
# 1. Install Firebase CLI
npm install -g firebase-tools

# 2. From this firebase/ directory, log in and link to a Firebase project
firebase login
firebase use --add        # pick an existing project or create one in the console

# 3. Install function deps
cd functions
npm install
```

You'll also need to enable Firestore (Native mode) and the Cloud Scheduler API in the Firebase/GCP console — the CLI will prompt or link you there on first deploy.

## Local dev

```bash
cd functions
npm run build:watch                # in one terminal — recompile TS
firebase emulators:start --only functions,firestore   # in another
```

Trigger the scheduled function manually from the emulator UI (http://localhost:4000) → Functions tab.

## Deploy

```bash
firebase deploy --only firestore:rules,firestore:indexes
firebase deploy --only functions
```

## Firestore layout

```
/products/{source__sku}
  {
    url, sku, name, brand, description,
    images: string[],
    lowPrice, highPrice, priceCurrency,
    source: "babycenter.mk",
    scrapedAt: Timestamp
  }

/scrape_runs/{autoId}
  { source, discovered, ok, skipped, failed, elapsedMs, finishedAt }
```

Doc IDs are stable (`${source}__${sku-or-hash}`), so each run upserts in place — no duplicates.

## Schedule

`scrapeBabyCenter` runs **every 6 hours** in Europe/Skopje. Edit the `schedule` field in `src/index.ts` to change (any [unix-cron expression](https://crontab.guru/) works, or natural language like "every 1 hours").

## Adding the other two sites

Bottom of `src/index.ts` has a TODO. Pattern:
1. Write `bsDiscoverProductUrls()` + `bsParseProductPage()` (or whatever) for bebesupermarket / libertabebecentar.
2. Export another `onSchedule(...)` with a different `source` tag.
3. Doc IDs already include `source`, so the same `/products` collection stays merged.

## Android app integration

In `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
}
```

In a Hilt module:
```kotlin
@Provides @Singleton
fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
    firestoreSettings = firestoreSettings { isPersistenceEnabled = true } // offline cache on
}
```

Then your repository emits a Flow off the snapshot listener — Firestore handles the cache, so the first read is instant from disk.

## Cost expectations

- 1× scheduled function invocation per 6h ≈ 120/month → free tier (2M / month)
- Firestore writes: ~1000 products × 4 runs/day × 30 = 120k writes/month → free tier is 20k/day = 600k/month
- Firestore reads from the app: 1 read per product per cold open, then cached → very cheap
- Outbound bandwidth: a few MB per scrape → trivial

Realistically you'll stay on Firebase's free Spark plan unless the app gets serious traction.

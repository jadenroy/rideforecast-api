# USD beta benchmark policy

## Eligibility and display gates

- New quote submissions accept only USD (case-insensitive, stored as `USD`). The web form already sends USD; the API now enforces it. Other currencies return HTTP 400.
- Benchmark and assessment queries accept an optional `currency`, defaulting to USD for existing clients. A non-USD value returns HTTP 400. Only stored USD observations are selected, case-insensitively.
- The selected window includes observation times from `asOf - days` through `asOf`, inclusive. Future-dated records are excluded until their observation time arrives. Filtering by currency and window happens in the database.
- A comparison needs **at least 20 eligible observations** in the selected cohort, and its **newest observation must be no more than 7 days old at `asOf`**. Twenty is a conservative beta product gate; it is not a statistical significance threshold, assurance of independent contributors, or evidence that the sample represents the market. The seven-day rule concerns the newest observation; older observations can still contribute within the selected 7/30/90-day window.
- Similar-trip selection uses the same minimum of 20. Below that, the existing explicit city-wide fallback applies. No trip-normalized or time-of-day forecast is implied. The sample's oldest and newest observation times describe the cohort actually used.
- The API no longer derives confidence from count alone. `confidence` is retained for compatibility, with `NO_DATA` for an empty sample and `NOT_ESTIMATED` otherwise. Even an eligible sample is only a descriptive historical summary.

## Response additions and states

Both benchmark and assessment responses include `currency`, `dataStatus`, `minimumObservations`, `maxLatestObservationAgeDays`, `asOf`, `oldestObservedAt`, and `latestObservedAt`. Times are UTC ISO-8601 instants. `asOf` is the query evaluation time, not the time a new quote was collected.

| `dataStatus` | Meaning | Numeric comparison fields |
| --- | --- | --- |
| `NO_DATA` | No eligible USD observations in the requested window/cohort | Null; observation times null |
| `INSUFFICIENT_DATA` | 1–19 eligible observations | Null; actual count and observation times retained |
| `STALE_DATA` | At least 20 observations, but newest is over 7 days old | Null; actual count and observation times retained |
| `AVAILABLE` | Both gates passed | Descriptive min, quartiles, median, mean and max |

When a small sample is also old, `INSUFFICIENT_DATA` takes precedence; its dates still expose its age. Assessment uses `INSUFFICIENT_DATA` for all three unavailable states, with no range, median, or percentage verdict. Its count is the actual prior sample count, not zero. Existing assessment enum names are preserved when eligible; web labels say below/within/above the observed range rather than claiming a validated local norm.

## Existing data and rollout

No schema migration, backfill, deletion, exchange-rate conversion, or relabeling is required. Existing non-USD rows stay unchanged and remain visible in recent/private histories with their original currency; they never enter USD benchmark or assessment calculations. Do not rewrite these rows as USD. The current index can serve the market prefix; verify PostgreSQL query plans at production scale before considering a separate index migration.

Suggested release order after review and publication approval:

1. Publish the web changes first. A response without verified USD/freshness metadata displays an explicit unavailable state, retaining quote entry and saving. This safely spans the API rollout.
2. Publish the API changes. Use an isolated test database for all write tests; do not seed production with test quotes.
3. Run read-only production checks for benchmark and assessment: metadata present, sparse samples suppressed, stale samples suppressed where available, non-USD query rejected. Verify the actual deployment versions and browser CORS. Confirm recent feeds retain original currencies.
4. If the API must roll back, the new web client fails closed on older responses. API rollback also removes new server currency enforcement; plan a targeted rollback retaining that protection.

All changes currently remain on local branches. No deployment is implied by passing local tests.

# RideForecast product specification
Reviewed October 24, 2026. Owner: Jaden. Status: proposed product baseline for review.

RideForecast obtains ride-price intelligence from permitted platform or licensed partner APIs. Users can choose a route, inspect current prices and pickup times, understand historical changes and eventually receive evaluated forecasts and opt-in alerts. Manual observations remain optional historical evidence. They are never the prerequisite for current quotes.

## Product areas
| Area | User outcome | Required evidence |
| --- | --- | --- |
| Tracker | Plan a route and compare current authorized quotes | Provider/product, price type, currency, charges, ETA, source time and expiry |
| Forecast | Decide whether a different departure window is useful | Licensed history/training rights, uncertainty, held-out baseline evaluation |
| Insights | Understand route/time patterns and coverage | Provenance, sample counts, time range, data gaps and permitted retention |

The responsive React client and future SwiftUI client share Spring Boot contracts. Keep credentials and provider calls on the backend. Preserve current routing, market checks, account ownership/cancellation safeguards, optional observations and the existing native prototype. No replacement stack is selected.

## Current implementation boundary
API PR17 at d3d570b41dcd7d63af7496ee9869d2ec29b674b2 and web PR45 at 60b11221e802171ce42f73f7c48696763ed85ff0 are draft, unmerged foundations at this checkpoint. They remove simulated fares in the proposed API, expose unavailable provider candidates and make observation entry optional in the proposed web journey. They do not provide authorized live prices. The deployed older API may lack /v1/providers; web45 handles that as unsupported, without a fake-price fallback.

This follow-up adds independent scoped-grant enforcement on top of PR17. Existing main releases remain the production baseline until separately released. A passing database health check is neither platform connectivity nor an account-flow test.

## MVP journeys and acceptance
1. Guest selects pickup/destination. Existing routing supplies road distance and duration, never a provider fare.
2. Backend resolves the approved provider/product/geographic scope. Research candidates remain distinct from actual local coverage.
3. Without an eligible source, show the exact unavailable reason and a safe retry or clearly labeled external provider-app action.
4. With one eligible source, show a single-source result and identify missing relevant providers. With two or more comparable eligible sources, show a comparison limited to that observed set.
5. Current quotes expire while the page is open. Refresh cancels superseded requests; a late response cannot overwrite the newer route or availability state.
6. Save/history remains account-owned and subject to source retention rights. Optional user observations remain a separate provenance class.
7. Forecast/alert controls become useful only after the corresponding data and permissions exist. No decorative forecast substitutes for validation.

Do not label a result cheapest when relevant providers are missing, currencies differ, fees are unknown, quote types are incomparable or a quote expired. Prefer descriptive ordering within an explicitly comparable set; identify coverage and missing data in the result.

## Normalized fare contract to implement before a real adapter
Use a versioned schema change. The present web45 client deliberately rejects an AVAILABLE schema it does not understand.

| Field | Meaning / rule |
| --- | --- |
| providerId, productId, marketId, coverageVersion | Exact approved source and scope; caller-supplied market labels do not authorize a trip |
| quoteId, priceType | ANONYMOUS_ESTIMATE, PERSONALIZED_FARE, PARTNER_PRICE, BOOKABLE_QUOTE or FINAL_RECEIPT |
| currency, amount or lower/upper | Monetary ISO currency and precise decimal values; absent never means zero; validate lower <= upper |
| includedCharges, excludedCharges, chargeCompleteness | Explicit mandatory-charge treatment; unknown charges prevent a total-price ranking |
| pickupEtaSeconds, tripDurationSeconds | Distinct pickup wait and driving time; null means unknown |
| sourceObservedAt, fetchedAt, expiresAt | Separate provider observation, retrieval and validity times; never invent provider timestamps or lifetime |
| sourceKind, sourceReference | DIRECT_API or LICENSED_PARTNER_API and non-secret traceability |
| personalization, membershipDiscounts, promotions | Explicit included / excluded / unknown context; never assume account offers |
| status, reason, retryable | Structured availability state with no numeric fare for missing/expired results |

Unavailable states: NOT_INTEGRATED, PERMISSION_REQUIRED, OUTSIDE_COVERAGE, NO_VEHICLES, TEMPORARY_FAILURE and EXPIRED. Do not reinterpret an unsupported API version as no vehicles. HTTP success alone does not establish a valid fare.

Production examples must come from authorized sources. Offline test data must stay in test resources, use synthetic providers and never be loaded by a production registry. No recording, caching, redistribution or training follows automatically from comparison approval.

## Next milestone
Review and release the unavailable-only API/web foundation through its existing owner, with exact-head CI, compatibility checks and exact deployed commit evidence. Then complete the independent-grant follow-up and source-specific scope validation before a real adapter.

A genuine comparison milestone requires two relevant authorized sources in one verified market. If agreements permit only one, ship an honestly labeled single-source pilot or wait; do not fabricate the second source.

## Later milestones
Licensed history precedes historical Insights. Forecasting requires chronological holdout evaluation against a declared baseline, error and interval-calibration evidence, and a useful decision threshold. Alerts require licensed monitoring, explicit opt-in, deduplication and pause/unsubscribe behavior. Native work resumes from the preserved SwiftUI prototype after Mac/Xcode access; web/backend work proceeds now.

Accounts have their own prerequisites and authorized test identities. Mapbox token restrictions and real-device handoff checks remain tracked separately. Source access, account readiness, deployment health and forecast accuracy must never collapse into one ready flag.

Source of direction: Jaden's October 24 handoff. Implementation evidence:
- https://github.com/jadenroy/rideforecast-api/pull/17
- https://github.com/jadenroy/rideforecast-web/pull/45

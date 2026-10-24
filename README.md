# RideForecast API

Spring Boot backend for RideForecast, served through `api.innersynapse.com`.

## Current MVP endpoints

### Health

`GET /health` and `GET /v1/ready`

Both routes acquire a database connection and run a bounded read against `quote_observations`. An empty table is ready. An unavailable database or missing quote table returns `503`, `Retry-After: 5`, and a generic status without connection details; a successful check returns `200` with `status: "ok"` and `database: "ready"`. Responses use `Cache-Control: no-store`.

The existing Render `/health` probe now reflects database readiness. The web client uses `/v1/ready` through the existing browser CORS policy. Connection acquisition is limited to five seconds and the query to two seconds. This verifies quote-table access, not every query or write path; check the real read endpoints after a release as well.

For a read-only release check, request `/health`, `/v1/ready`, `/v1/quotes/benchmark`, `/v1/quotes/assess`, and `/v1/quotes/recent` with an existing public market. Confirm HTTP status, USD/evidence metadata, and CORS for the canonical frontend. Record first-request and subsequent request durations separately. Do not write synthetic observations to production or infer a cold start solely from a slow response. Free-service inactivity can still cause startup delay; this change adds truthful status and retry behavior without an upgrade or background keepalive.

### Provider availability and legacy prices

`GET /v1/providers` reports the non-exhaustive provider registry, separate endpoint capability,
production access, comparison/retention/training permissions, reviewed evidence and approved
market/product/currency coverage. All entries currently return `UNAVAILABLE`; no live fare
integration is configured or approved. This status is independent of quote database readiness.
The endpoint needs no location and returns `Cache-Control: no-store`.

`POST /v1/prices`

The old formula-generated Uber/Lyft prices have been removed. This legacy endpoint retains
`source`, `fetchedAt`, and `rides`, but returns an empty `rides` array, `source: "unavailable"`,
`status: "UNAVAILABLE"`, `reason: "NO_APPROVED_PROVIDER_INTEGRATION"`, and
`availabilityUrl: "/v1/providers"`. Its timestamp is response time, not an upstream fare timestamp.
HTTP 200 means the request was handled, not that fares are available. Clients must handle empty
results. The current web observation flow does not call this endpoint; the frozen Swift prototype
may require an empty-state update before use. No fabricated fallback or zero-dollar fare is emitted.

`ProviderAdapter` defines transient normalized fares with provider, product, market, currency,
amount, direct/partner provenance, source reference, fetch time and expiry. `ProviderGateway`
checks documented capability, production/comparison approval, dated unexpired evidence and exact
coverage before calling an adapter, then rejects invalid or stale results. Coverage includes explicit
approved geographic rectangles: both pickup and destination must be finite, valid coordinates inside
the approved bounds, regardless of caller market labels. Route distance and duration must be finite
and positive. Non-monetary ISO codes (including XXX and XTS) are rejected. Rectangles must be wholly
within approved territory; an irregular area requires a stricter provider-specific resolver before
an adapter is introduced. Endpoint containment does not establish permission for an entire route.
Retention and training
are separate permissions and are not implied by comparison approval. No provider data is persisted,
cached, logged or used for training here. Research links are evidence of investigation, not grants.
There are no production adapters and no public adapter-execution route in this increment.
The current policy has one shared evidence record and expiry for production/comparison approval;
it does not implement independent evidence or expiry scopes for each permission. Separate grants
must be modeled and verified before enabling retention or training.

Before adding one: obtain written rights covering the specific use and markets, record reviewed
approval scope/expiry (without publishing private agreements), implement timeouts and provider-specific
normalization, validate in an approved sandbox, then connect the web availability and fare UI.
An API key or sandbox account alone cannot enable comparisons. Existing user observations remain
separate historical evidence and are never substituted for live API fares.

### Log an observed quote

`POST /v1/quotes`

Stores a quote a user actually saw in a provider app. The record includes provider, ride type, quoted price, origin/destination coordinates, local market, optional zones, road distance, ETA, observation time, and source.

Public submissions are protected by:

- `Idempotency-Key`: the web app reuses one key when retrying the same save. A new observation returns `201` with `Idempotency-Replayed: false`; a safe replay returns the original observation with `200` and `Idempotency-Replayed: true`.
- Persistent payload fingerprints: clients without the header still receive duplicate protection for an identical payload, including across API restarts.
- Server validation: prices are limited to `$0.01–$10,000`, coordinates must be valid, distance is limited to `1,000` miles, ETA is limited to `1,440` minutes, providers and currency codes are validated, and future observation times are rejected.
- Rate limiting: the API accepts at most 12 quote attempts per client per minute by default. Configure `QUOTE_SUBMISSION_MAX_PER_MINUTE` to change that limit. Rate-limited requests return `429`, `Retry-After`, and a structured error.
- A hidden `website` honeypot field: non-empty values are rejected as automated submissions.

The rate-limit window is held by the running API instance. PostgreSQL idempotency is the durable layer that prevents repeated saves after a restart.

Example:

```json
{
  "provider": "Uber",
  "rideType": "UberX",
  "quotedPrice": 14.83,
  "currency": "USD",
  "originLatitude": 35.687,
  "originLongitude": -105.938,
  "destinationLatitude": 35.617,
  "destinationLongitude": -106.089,
  "marketCity": "Santa Fe",
  "marketRegion": "NM",
  "marketCountry": "US",
  "originZone": "Downtown",
  "destinationZone": "Airport",
  "roadDistanceMiles": 10.4,
  "travelMinutes": 18,
  "source": "user-observed",
  "website": ""
}
```

### Recent market observations

`GET /v1/quotes/recent`

Required query parameters: `marketCity`, `marketRegion`, `marketCountry`.

### Local benchmark

`GET /v1/quotes/benchmark`

Required: `marketCity`, `marketRegion`, `marketCountry`, `provider`, `rideType`.

Optional: `days`, `distanceMiles`, `travelMinutes`, `originZone`, `destinationZone`.

When enough comparable observations exist, RideForecast prefers the same local market plus similar trip distance/duration (and zones when supplied). If there are too few similar trips, the response explicitly labels the result as a market-only fallback.

The descriptive range uses the 25th to 75th percentile of eligible USD observations only after the beta gates pass: at least 20 observations, with the newest no more than 7 days old. Empty, insufficient, and stale samples return null statistics with their actual count and observation dates. See [BENCHMARK_POLICY.md](BENCHMARK_POLICY.md) for response fields, currency validation, historical-data handling, limitations, and rollout order.

### Quote assessment

`GET /v1/quotes/assess`

Adds `quotedPrice` to the benchmark request and returns one of:

- `BELOW_LOCAL_NORM`
- `WITHIN_LOCAL_NORM`
- `ABOVE_LOCAL_NORM`
- `INSUFFICIENT_DATA`

When eligible, the API returns the percent difference from the observed median, count, and comparison scope. Confidence is not inferred from count (`NOT_ESTIMATED`). Inadequate or stale samples receive `INSUFFICIENT_DATA` without a percentage or local-norm verdict; their actual count and freshness metadata remain available.

## Important product framing

RideForecast should describe these results as observed-market price intelligence, not proof that a provider is deceptive or fraudulent. The benchmark answers: **"How unusual is this quote compared with recent comparable observations in this market?"**

## Accounts and profiles

The account foundation uses Firebase Authentication for identity verification and PostgreSQL for RideForecast-owned profile data. Private endpoints are `GET`, `PUT`, and `DELETE /v1/profile`, plus `GET /v1/profile/quotes`. A valid Firebase ID token must be sent as `Authorization: Bearer <token>`. Signed-in quote saves are attached to that profile; public market responses never expose ownership.

Production variables are `AUTH_ENABLED`, `ALLOW_ANONYMOUS_QUOTES`, and the secret `FIREBASE_SERVICE_ACCOUNT_JSON`. Defaults preserve the current guest beta until provider acceptance is complete. See [AUTHENTICATION.md](AUTHENTICATION.md) and the web repository's release checklist.

## Persistence

The app supports PostgreSQL through environment variables:

- `JDBC_DATABASE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO` (defaults to `update`)

Without those variables, the service falls back to an in-memory H2 database for development. H2 data is lost when the service restarts, so production quote collection should use PostgreSQL.

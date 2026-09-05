# RideForecast API

Spring Boot backend for RideForecast, served through `api.innersynapse.com`.

## Current MVP endpoints

### Health

`GET /health`

### Simulated ride prices

`POST /v1/prices`

Returns the current server-side simulated Uber/Lyft prices for a trip.

### Log an observed quote

`POST /v1/quotes`

Stores a quote a user actually saw in a provider app. The record includes provider, ride type, quoted price, origin/destination coordinates, local market, optional zones, road distance, ETA, observation time, and source.

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
  "source": "user-observed"
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

The expected range currently uses the 25th to 75th percentile of observed quotes.

### Quote assessment

`GET /v1/quotes/assess`

Adds `quotedPrice` to the benchmark request and returns one of:

- `BELOW_LOCAL_NORM`
- `WITHIN_LOCAL_NORM`
- `ABOVE_LOCAL_NORM`
- `INSUFFICIENT_DATA`

The API also returns the percent difference from the local median, observation count, comparison scope, and confidence level.

## Important product framing

RideForecast should describe these results as observed-market price intelligence, not proof that a provider is deceptive or fraudulent. The benchmark answers: **"How unusual is this quote compared with recent comparable observations in this market?"**

## Persistence

The app supports PostgreSQL through environment variables:

- `JDBC_DATABASE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO` (defaults to `update`)

Without those variables, the service falls back to an in-memory H2 database for development. H2 data is lost when the service restarts, so production quote collection should use PostgreSQL.

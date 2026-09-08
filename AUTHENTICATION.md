# Profile and authentication API

The full product and rollout contract lives in the web repository's `AUTHENTICATION.md`. This API keeps RideForecast data in PostgreSQL and accepts Firebase Authentication only as proof of identity.

## Data boundary

- `user_profiles` stores an internal ID, Firebase UID, preferred name, optional home market, acceptance versions and times, and timestamps.
- `quote_observations.owner_id` is nullable so existing controlled-beta observations remain valid.
- Email, phone number, provider access tokens, and passwords are not stored in PostgreSQL.
- Public market endpoints never expose an owner ID.

## Runtime behavior

- With `AUTH_ENABLED=false`, Firebase is not initialized and the existing public API continues to start normally.
- With `AUTH_ENABLED=true`, bearer tokens are verified with the Firebase Admin SDK.
- With `ALLOW_ANONYMOUS_QUOTES=true`, controlled-beta quote writes may remain anonymous. Set it to `false` for the account release.
- Private profile endpoints always require a valid token regardless of anonymous quote mode.

Render must store `FIREBASE_SERVICE_ACCOUNT_JSON` as a secret environment value. The service account JSON must never be committed, logged, returned to the browser, or copied into Vite variables.

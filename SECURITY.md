# RideForecast API security

## Supported version

Security fixes are applied to the current `main` branch and the production service deployed from it.

## Reporting a vulnerability

Do not post credentials, private rider information, or a working exploit in a public issue. Use the repository's **Security** tab to report a vulnerability privately. Include the affected endpoint, the impact, and the smallest safe reproduction you can provide.

## Production baseline

- Firebase ID tokens are checked for signature, issuer, audience, expiration, revocation, and disabled-user state.
- Account deletion requires a Firebase authentication time within the last five minutes before PostgreSQL data can be removed.
- Private profile responses are marked `Cache-Control: no-store`.
- Route coordinates are rounded to three decimal places before persistence and are omitted from public and private API responses.
- Quote submissions have strict field limits, a honeypot, per-client throttling, idempotency keys, and database uniqueness protection.
- Query inputs have explicit lengths and provider allowlists.
- CORS uses exact origins and bearer tokens instead of browser cookies.
- The production container runs as an unprivileged operating-system user.
- Browser security headers are applied by the API filter.
- CI actions are pinned to commit hashes; CodeQL runs on pull requests, `main`, and weekly; Dependabot monitors Maven and GitHub Actions dependencies.

Operational credentials belong only in Render secret environment values. Never place a Firebase service-account JSON, database password, or private provider key in source control or a `VITE_*` variable.

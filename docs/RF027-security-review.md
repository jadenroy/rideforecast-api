# RF-027 source audit and ownership safeguard — 2026-10-25

## Follow-up: expected insert conflict application logging

The next milestone starts from API main `b48a8cac23333fb5b139fdcd70a0c33e4631fb2d`; web main remains `290ab28174b159833fc010633e698446f18687a0`. Open PR and owner checks found only the 13 dependency PRs and idle previous RideForecast owners. No repository AGENTS.md/.agents instructions were present.

Baseline reproduction: `mvn -o --batch-mode -Dtest=QuoteSubmissionProtectionTests#databaseUniquenessProtectsSimultaneousDuplicateWrites test` passed its HTTP assertions but emitted the literal synthetic key through `org.hibernate.orm.jdbc.error`. The warning happens inside Hibernate before the service catch, so catching or redacting the final HTTP error cannot remove it.

The candidate uses a dedicated repository insert fragment with a parameterized JDBC statement and its own auto-commit connection. This keeps a failed insert separate from the subsequent replay read and out of Hibernate's exception logging. SQLSTATE 23505 becomes a value-free `QuoteInsertConflictException` and the fixed operational event `QUOTE_INSERT_UNIQUE_CONFLICT sqlState=23505`, without retaining driver message/cause. Only this typed exception is eligible for recovery; other SQL failures retain their translated diagnostic and propagate. A unique conflict without a matching stored submission still fails. No logger levels, SQL settings, schema, key format, or stored records are changed.

Both initial and racing replay paths still check the stored owner and fingerprint before returning a result. The global unique index remains authoritative. The fragment is intentionally an independent insert, not an operation to compose inside a larger atomic transaction. Current quote-save callers have no outer transaction; future callers must preserve that boundary or explicitly redesign it. A future model column change must also update the explicit insert mapping.

Regression coverage includes a guaranteed pair of direct inserts with the same synthetic key, root log capture, marker absence, a safe conflict event, and an unrelated Hibernate warning that remains visible. A real NOT NULL failure retains SQLSTATE 23502 and its driver cause. Deterministic service races use a barrier after both empty lookups and then real H2 inserts: same-owner replay, changed payload conflict, account/account, account/guest and guest/account rejection. Existing HTTP generic-409, owner-history, auto-key, validation and account guards remain in the full suite. Two additional unit checks forbid recovery for unrelated integrity failures or unique conflicts without a matching submission.

Evidence limits: these tests establish application logging and H2 concurrency behavior, not production PostgreSQL concurrency. A disposable PostgreSQL 18 cluster was attempted using the existing installation; default initialization failed for insufficient disk space. A reduced-WAL test cluster initialized and briefly started on 127.0.0.1:55432, but exhausted C: space before tests. It was stopped immediately. Relocation to `D:\Codex-RF027-task12-pg` failed with OS access denied; no retry or permission change was attempted. The stopped test cluster was removed and destination absence checked. No PostgreSQL test result is claimed. No production database, real identifiers/accounts/providers, credentials, infrastructure or security settings were changed.

Local validation on Java 21 / Maven 3.9.11: `mvn -o --batch-mode test` passed **82 tests, 0 failures, 0 errors, 0 skipped**. `git diff --check` passed. The reproduced baseline and final output are retained in task-12 `baseline-unsandboxed.log` and `verified-h2-tests.log`; the latter contains fixed conflict events and no `quote-test-key-concurrent` or generated synthetic private/service-race key values. The disk-interrupted intermediate run is not counted as validation.

This patch covers the expected unique-conflict application/framework path under normal production log levels. Database-server error logs can still contain constraint values; JDBC wire/parameter tracing, unrelated database errors, access/edge logs and broader log redaction remain separate work. No claim of complete log privacy is made. RF-027 stays In progress with rows 117/121 Partial; RF-026 actual speech remains Partial. Parent independent review and exact-head CI are required before release.

## Scope and acceptance

Reviewed web `290ab28174b159833fc010633e698446f18687a0` and API `500164124fb4b2476703c5a7b00b08517114d184`. Fresh clones matched remote main. Prior RF-026 owner was idle after PR49 release; all 13 open PRs were dependency updates. No repository AGENTS.md or .agents instructions were found. Render readback confirmed web `dep-dbesbheq1p3s73ei9qvg` and API `dep-dbeeq6mq1p3s73e6iuf0` live on these refs. This is source analysis and synthetic testing, not a production penetration test or live settings certification.

Exact tracker criteria, Acceptance checklist rows 117–121:

1. Review server-only secrets, location minimization, auth boundaries and ownership controls.
2. Specify permission-aware logs/caches with sensitive data redaction and retention.
3. Define server rate limits, upstream quotas, request validation and least-privilege credentials.
4. Review dependency/security findings and evaluate Dependabot PRs individually, without blind merging.
5. Prepare actionable fixes and tests; do not change account access or security settings under board creation.

## Threat and data-flow inventory

| Boundary | Data and current controls | Residual issue / next action |
|---|---|---|
| Browser → Mapbox / map tiles | `location.ts` sends search text, session token, optional proximity, precise route endpoints; fallback proximity is IP. `RouteMap.tsx` requests viewport tiles from Mapbox or OpenStreetMap. GPS is user initiated. Requests support abort. | Third parties receive IP/viewport/search/coordinates before a quote save. Review disclosure and minimize proximity precision when adequate; do not claim no location transmission. No GPS permission or actual requests made in this audit. |
| Browser storage | `tripDraft.ts` stores exact draft addresses/coordinates in sessionStorage; `authClient.ts` selects Firebase browserLocalPersistence. | Draft is shared across account sessions by product design; it is not private account storage. Define expiry/discard UX and a shared-device policy before changing behavior. Do not persist provider fare responses or tokens in the trip draft. |
| Browser → quote API | Benchmark GETs contain market, zones, distance/time and optional price; saves send precise coordinates, rounded to 3 decimals in the service before storage. Response DTO excludes coordinates and owner. | Precise coordinates still transit to API; removing them requires contract/migration review. URLs may appear in infrastructure logs. Public recent responses still include zone/time/price and can be identifying in sparse markets; aggregate-only display is a separate product decision. |
| Firebase → API identity | Server verifies ID tokens with revocation check. Profiles/history derive owner from verified UID, not a client owner field. Delete requires recent auth and removes only that owner's records. | Real-account release and actual IAM/API-key restrictions remain unverified, intentionally untouched. |
| Quote retries → PostgreSQL | Global unique submission key, payload fingerprint, concurrent insert recovery. | **Fixed in this candidate:** both replay paths now require equal owner IDs before returning success. Account→other account, account→guest and guest→account collisions fail with generic 409. |
| Async browser requests | App aborts reads and guards quote/account results by session generation; AuthProvider checks identity before sensitive operations. | Cancellation suppresses stale UI; it cannot undo an already dispatched server write or guarantee an upstream call was not billed. Preserve idempotent uncertain-save retry. |
| API → future providers | Registry is unavailable; gateway checks approval, scoped grants, geography and returned fare validity/expiry, and fails closed on runtime failure. Results are transient. | No adapters activated. Gateway has no deadline/concurrency/budget enforcement; these must precede any real adapter. |

## Selected safeguard and verification contract

`QuoteObservationService.replay` previously checked only the fingerprint. A second account using the same key and payload received a successful replay of the first account's observation, without acquiring an owned record. The candidate checks owner equality on initial lookup and uniqueness-conflict recovery. It preserves stored records and keys, requires no schema migration, and uses the existing `IDEMPOTENCY_KEY_REUSED` 409 with no owner identifier in the error. Anonymous→anonymous remains the existing anonymous namespace: this is not anonymous-user authentication. Global collisions still require a fresh key; this patch does not create per-account key namespaces.

`QuoteReplayOwnershipTests` deterministically covers explicit and auto keys, both lookup paths, three unequal-owner directions and successful same-owner/anonymous retries. Baseline: 5 parameterized tests, 3 failures (each unequal-owner case returned success). Fixed unit cases cover 12 rejected combinations and 4 valid retries. `ProfileAuthenticationTests.quoteRetryCannotCrossAuthenticatedAccounts` adds HTTP 201→409→same-owner 200, empty other-account history, unchanged owner and row count. Existing concurrency, auth, deletion, provider, grant, validation and security-header tests run in the full suite. All fixtures are synthetic, in-memory H2 / mocked repositories and fake token verifier. No production writes or provider calls.

Local final validation: Java 21, Maven 3.9.11, offline `mvn --batch-mode test`: **73 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. `git diff --check` passed. Initial sandbox test process could not attach normally; the approved offline run using the existing cache reproduced the baseline and passed the fixed suite. No web code changed. Parent independent review and exact-head CI remain release gates.

## Logs, caches, retention specification

Source scan found no explicit application logger/console statements in main API code. This does **not** establish safe framework/edge logs: the synthetic concurrent-insert test produced a Hibernate warning containing the idempotency key. Next scoped fix: replace/suppress value-bearing database diagnostics with safe event codes, then capture logs in regression tests and assert absence of synthetic tokens, keys, UIDs, coordinates and request bodies. Do not print exception causes from credential parsing or upstream responses into user-visible/log output.

Allowlist operational fields: generated request correlation ID, route template (never full query), status, coarse duration bucket, provider ID and approved reason code. Exclude Authorization, cookies, service-account JSON, DB credentials, Mapbox token URL query, addresses, coordinates, quote bodies, UID and idempotency keys. Scrub before serialization. Proposed short diagnostic retention: 7 days, aggregated non-identifying operational counters 30 days, restricted staff access; confirm legal/provider conditions and operator approval before configuration changes.

Private profile paths currently receive no-store even on errors; prices success is no-store. Extend no-store consistently to all identity-sensitive/write-error paths in a separate tested increment. Never cache provider data without an explicit cache grant: scope cache keys by provider/market/product/currency and permission version; TTL must be no later than fare/grant expiry; permission revocation must invalidate access. Raw provider responses should not enter logs, cache or history. User observations currently have no time-based deletion job. RF-028 must define approved retention windows, backups and deletion semantics before any destructive job; no retention operation is authorized by this audit.

## Abuse, validation and credential plan

Current quote writes: 12 attempts/minute per remote address, 60-second in-memory window, 429 + Retry-After, honeypot, DTO lengths/ranges, timestamp and idempotency checks. Limit state is per process, resets on restart, has no hard cardinality cap, and its periodic cleanup is request driven. Native forwarded headers are enabled; proxy trust configuration has not been verified. Do not trust a raw client-supplied X-Forwarded-For or claim a distributed quota.

Ready follow-ups: bound limiter key count and cleanup safely under concurrency; add measured per-identity and trusted-IP budgets for reads/auth/profile writes; bound benchmark DB result count (current query can load all matching observations in up to 365 days); add total JSON body-byte limits including unknown/chunked bodies; bound `PriceRequest.destinationLabel/requestedAt`, coordinate validity and maximum trip dimensions at ingress. Bean field validation runs after parsing and is not a total request-size limit. Use synthetic saturation/expiry/concurrency fixtures and a mutable clock, not production load.

Before any provider activation: zero permitted calls until signed grants, least-privilege credential and configured provider budget all exist. Reserve budget atomically before dispatch; cap concurrency, deadline, daily spend/call count and response bytes; count retries against quota; no automatic retry for non-idempotent upstream actions; respect Retry-After; cancellation must propagate where supported while still charging already dispatched work. Quota denial returns UNAVAILABLE without fabricated fares. Exact numerical budgets must come from provider contracts and approved spending, not guessed defaults.

Firebase browser API keys identify a project; they are not service-account secrets ([official guidance](https://firebase.google.com/docs/projects/api-keys)). Mapbox public tokens are designed for clients; secret tokens must stay server-side ([official guidance](https://docs.mapbox.com/help/dive-deeper/access-tokens/)). `VITE_*` is public. Firebase service-account JSON, DB passwords, private provider keys and deploy hooks belong only in server secret storage with minimum roles. No live secret environment values were requested. Actual restrictions/permissions need operator confirmation before changes; no credentials were rotated or account/network settings changed.

## Dependency findings and individual PR decisions

GitHub authenticated read on 2026-10-25: web has 14 open alerts (4 high, 8 medium, 2 low); API has 0 open alerts, which is not proof of no vulnerabilities. Exact sanitized advisory metadata is in `rf027-web-advisories.json`. Lockfile confirms Vite 8.2.2, Vitest/mocker 5.0.0, source-map-js 1.2.1, undici 8.10.2 and grpc-js 1.9.16. Production web is a Render static site publishing dist; it does not serve Vite or a Node gRPC server. Source imports Firebase app/auth, not Firestore. gRPC is brought by Firebase Firestore in the full package lock; no server getAuthContext authorization path was found. This lowers observed runtime reachability, not upgrade priority or a formal bundle proof.

| Group | Advisory IDs / patched version | Exposure and action |
|---|---|---|
| Vite (6) | GHSA-9jrq-w75r-8gcw, rq7h-c2jc-7f22, vfpm-58rq-9qcg, r825-w8mm-5pxw, 587h-7wmj-h5pm, 698v-4r3x-3vcv; 8.2.4 | Dev-server/editor/file-access issues. Prioritize a scoped tooling update; never expose dev server to untrusted networks. [Windows editor advisory](https://github.com/advisories/GHSA-587h-7wmj-h5pm). |
| Vitest mocker (2) | GHSA-qxg6-89mg-rqv8, xp26-3454-mjgp; 5.0.3 | Reachable mock server can allow injection/traversal. [Advisory](https://github.com/advisories/GHSA-qxg6-89mg-rqv8). PR35 moves mocker to 5.0.5 but leaves Vite 8.2.2. |
| undici (3) | GHSA-xjj6-f33q-929c, 4qv8-2p9j-2685, f72v-6w45-q472; 8.12.2 | jsdom development dependency; malicious response can crash Node, unbounded EventSource/SRI issues. [DoS advisory](https://github.com/advisories/GHSA-xjj6-f33q-929c). No actual hostile fetch performed. |
| source-map-js (1) | GHSA-68fv-2mgg-jv7q; 1.2.2 | Build/test dependencies css-tree/PostCSS; malicious source-map offsets can block event loop. [Advisory](https://github.com/advisories/GHSA-68fv-2mgg-jv7q). |
| grpc-js (2) | GHSA-m9gg-hp2v-232j, f596-whhp-79r4; 1.13.6 (also relevant 1.14.5 branch) | Server certificate auth/error disclosure paths not found in this static frontend. [Auth advisory](https://github.com/advisories/GHSA-m9gg-hp2v-232j). Still update the resolved dependency after compatibility review. |

All 13 PR patches inspected individually; none merged or executed:

| PR | Decision / required verification |
|---|---|
| web #35 Vitest 5.0.5 | Best existing security-related update candidate; patches mocker, but does not remove all alerts. Rebase and run full web/offline/build/browser checks. |
| web #33 Firebase 13.1.0 | Major SDK/Node engine change (>=24.12.0 in changed packages); patch still references grpc-js 1.9.16. Do not treat as a gRPC fix. Require auth compatibility tests; no live SMS/account calls. |
| web #34 Node types 26 | Types/runtime mismatch with current CI Node 24; not a security fix. Defer pending runtime plan. |
| web #30 TypeScript 7 | Native compiler package/platform changes; separate migration, not alert remediation. |
| web #36 oxlint 1.89 | Lint/tooling update, no mapped alert; verify rule changes separately. |
| web #29 setup-node 7 | Pinned action update; inspect runner/runtime/cache changes and fresh CI. No application alert closure inferred. |
| web #28 checkout 7.0.1 | Pinned action major update; verify credentials/checkout and runner requirements. |
| web #27 pnpm action 6.1 | Pinned action update; preserve packageManager 11.19.0/frozen lock and validate installer/cache behavior. |
| API #14 Firebase Admin 9.11 | POM-only bump with transitive library changes; no open API alert maps to it. Resolve dependency tree and rerun auth/provider suite independently. |
| API #13 setup-java 6.0.1 | Changes CI and CodeQL pins; keep Java 21 and verify both workflows/runtime compatibility. |
| API #10 checkout 7.0.1 | Changes both workflows; review credential handling/runner requirements and fresh checks. |
| API #8 CodeQL init | Pin-only init update; coordinate with analyze #9 to keep versions aligned. |
| API #9 CodeQL analyze | Pin-only analyze update; coordinate with init #8 and verify scan uploads/permissions. |

Next ready goal after ownership release: scoped web tooling security update (Vite/mocker/undici/source-map-js), using #35 as reviewed input, with lockfile reachability and exact-head CI. Preserve RF-026 Partial solely for actual speech verification: DOM announcements are not spoken-output evidence. Provider permissions remain unavailable; RF-028 licensed retention is not ready for destructive execution.

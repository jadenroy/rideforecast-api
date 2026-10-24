# Independent provider permission grants
Status: implemented in this follow-up, pending review/release. No production grants or adapters are configured.

The public research registry and the existing policy summary remain backward compatible. They are not authority to invoke an adapter. ProviderPolicy now holds a server-reviewed immutable map of separate grants. The map is excluded from JSON serialization, so private evidence references are not added to the public /v1/providers payload.

## Enforcement
ProviderGateway still requires documented capability, legacy policy approval, current summary evidence, exact coverage and valid trip geography/metrics. In addition it requires independent PRODUCTION_ACCESS and COMPARISON grants before invoking an adapter, and evaluates them again when a response arrives. The legacy constructor creates no grants, so approved summary flags alone fail closed.

Each grant records the provider, explicit approval state, opaque evidence reference, review time, activation, expiry, optional revocation time and exact coverage tuples. Missing evidence, future review/activation, expired approval, effective revocation or scope/provider mismatch denies that right. Expiry and revocation boundaries are exclusive: a grant ending at the current instant is no longer usable.

CACHING, RETENTION, REDISTRIBUTION and FORECAST_TRAINING are independent rights. Their operations must explicitly request their full required-right set at the point of use. No such persistence, cache or model operation is introduced here. A future retention/training pipeline must also enforce source-specific permitted fields, periods, deletion, geography and downstream display constraints.

## Remaining boundaries
The shared summary evidence field remains a conservative compatibility gate; its validity does not replace the new independent grants. A grant references the same exact approved geography tuple used by the gateway. Rectangular endpoint containment does not prove an entire route is authorized, resolve irregular boundaries or establish airport eligibility. A real adapter must add a source-specific resolver before activation.

Grant maps are immutable configuration snapshots. Scheduled revocation times are checked, but this increment does not implement a dynamic approval/revocation store or reload protocol. A future real-source activation must define how updated or immediately revoked authority reaches every request and in-flight result.

The public registry's coarse permission summaries are not a public authorization decision for a specific trip. Do not expose private grant contents or accept caller-supplied grant maps. No production fixture, account setting, source credential, storage change or upstream call is added.

## Acceptance evidence
Offline tests cover every right, independence of comparison from other uses, separate expiry, activation/review dates, revocation, provider/scope mismatch, immutable configuration and denial before adapter invocation. An in-flight expiry test proves a previously authorized comparison is discarded when its own grant expires during the upstream request. Existing gateway, HTTP/CORS, account and quote tests remain part of Maven verification.

Future clients use the same backend. No grant logic belongs in a mobile client as a substitute for server enforcement.

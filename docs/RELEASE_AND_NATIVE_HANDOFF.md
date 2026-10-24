# Release and native handoff
October 24, 2026. Preparation only; no merge/deployment in this follow-up.

## Exact baseline
| Component | Reviewed branch checkpoint | State at inspection |
| --- | --- | --- |
| API foundation PR17 | d3d570b41dcd7d63af7496ee9869d2ec29b674b2 | Open draft, unmerged |
| Web availability PR45 | 60b11221e802171ce42f73f7c48696763ed85ff0 | Open draft, unmerged |
| API main | 1fffe0ef296da76ef921ef9dd7683fb26a8d6543 | Existing production baseline reported by owner; current git remote verified |
| Web main | c673a6a1974732a1b928497eaab4d735163087f3 | Existing production baseline; PR45 base verified |

Use current heads, not this dated table, for a release decision. Implementation/release owner of PR17/45 remains dot task 01a16d57-95c6-7066-bae4-d27a9e8ac017. This follow-up is stacked on PR17 and must be reviewed for its delta separately.

## Release checks
- Record the exact authorized scope, final heads, passing checks and reviewer findings.
- If a head changes, validate the changed head before release.
- Release the API before the web when practical. Web45 must retain its safe older-API 404 state for rollout/rollback.
- Verify post-merge main CI, terminal deployment states and exact deployed commits separately.
- Read-only verify /health, /v1/ready and /v1/providers. An unavailable result is correct while no real integration is authorized.
- Validate legacy /v1/prices empty-rides compatibility in an isolated environment. Do not send test observations to production.
- Check primary planning flow, expiry/retry/cancellation and optional historical recording on desktop/mobile.
- Rollback must preserve empty/unavailable semantics and source honesty. Do not restore formula-generated fares as a fallback.

Actual Google/Apple/phone profile/save/history smoke requires configured providers and an authorized identity. Those tests remain separate from guest routing and provider fare access.

## Native handoff
Keep the original SwiftUI source and document its location before editing it. Preserve live location, saved places, route/timeline work and existing layout where useful. Do not choose a replacement framework.

The future client must handle empty rides, unsupported endpoints, unavailable states, refresh cancellation and source expiry. A future AVAILABLE contract is a versioned client change; the current unavailable-only web client is not evidence that live fares already work.

On a Mac, verify the preserved prototype's toolchain and dependency versions, configure the shared backend, and run explicit real-device location, auth, accessibility and cancellation checks. Do not copy source credentials into the app. No current Mac/Xcode availability is assumed.

## Pilot and admin preparation
Use Houston only as a provisional diligence target until partner coverage and rights support the choice. Prepare pilot tasks around route entry, honest coverage, current-source freshness and useful comparisons. No tester contact is authorized by this document.

For optional observations, define administrative find/filter/flag/correct/remove operations with explicit ownership/admin authorization, a correction trail and rate limiting. Preserve genuine records; test corrections/deletions only on designated fixtures. This is a design boundary, not a shipped administrative console.

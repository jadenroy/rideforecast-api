# Provider access matrix and partner briefs
Checked October 24, 2026 against the primary sources below. Public documentation establishes capabilities or a commercial route, not a grant to RideForecast. No provider approval, credentials, application submission or paid access was obtained in this work.

## Access matrix
| Provider | Documented route | Exact remaining commercial/access questions | RideForecast state |
| --- | --- | --- | --- |
| Uber | Price and pickup-time estimates require approval through Uber's contact/business-development route [U1,U2] | March 2026 terms III.G require written permission for competitor aggregation, bulk pricing and model-training uses [U3]. Confirm permitted products/markets, comparative display, reuse, limits and fees. | No approved comparison feed |
| Lyft | Business/Concierge integration and program-to-client connections [L1,L2] | General Business terms are scoped and restrictive [L3]. Confirm an agreement allowing consumer comparison, data display, caching, history and forecasting, plus charges and quotas. | Business route documented; consumer quote access unverified |
| Waymo | Rider service and in-app pricing are documented [W1] | No public third-party fare/ETA interface was verified in the reviewed official material. Ask for an approved partner route and exact use/coverage rights. | No verified fare integration |
| Tesla Robotaxi | Rider app shows fare/wait information [T1]. Fleet API concerns Tesla products/vehicles [T2] | No public rider-pricing interface was verified. Vehicle-pricing scope is not a ride-fare endpoint. Ask for a specific Robotaxi partner interface and rights. | No verified fare integration |
| Curb Flow | Taxi supply/demand partnership network [C1] | Obtain technical quote/ETA specifications, actual production fleets, comparison/display rights, commercial terms, quotas and retention/training terms. Listed partners do not promise their consumer fares. | Partner candidate |
| Karhoo | Demand APIs describe quotes, ETA and a separate booking flow [K1]; production access requires contact [K2] | Verify current payload/version, active fleets in candidate cities, permitted comparison, fees/quotas and independent retention/forecasting rights. | Documented quote route; commercial access unverified |

For every row, production access, comparative display, caching, historical retention, redistribution and forecast training remain independently unapproved or unknown for RideForecast. No fee, rate limit, coverage boundary, quote lifetime or contract exception is invented here.

Uber deep links return neither price nor ETA [U4]. Routing and provider-app links remain separate capabilities. Waymo research material and Tesla vehicle telemetry are not substitute fare feeds.

## Launch-market recommendation
Provisional diligence priority: Houston, with Phoenix/Tempe as an alternative and Santa Fe as the existing routing/regression market. This is a product recommendation, not a launch decision.

Official Waymo material identifies Houston and Phoenix rider service [W1]; Tesla identifies limited Houston service [T1]. That makes Houston relevant to the intended provider mix, but establishes zero third-party feed permissions. Curb's reviewed app service-area page does not establish eHail coverage in Houston, Phoenix or Santa Fe [C2]; do not infer Flow contract coverage from it. Karhoo fleet overlap still needs written confirmation.

No candidate market is approved for a multi-provider launch yet. The selection gate is two relevant authorized, comparable quote sources with verified pickup/destination/airport rules, enough vehicle availability, acceptable costs and explicit data-use rights. If partner responses favor another market, revise this recommendation before implementation. No controlled manual-quote collection campaign is the core product strategy.

## Draft partner briefs
These are prepared questions for Jaden's review. No recipient or submission is approved by their inclusion here.

Common product description: RideForecast is a web-first ride-price intelligence product, later shared with a native SwiftUI client. It seeks permitted current quote/ETA display, transparent comparison and, only where licensed, history, forecasting and opt-in alerts. It does not request booking authority in the initial integration.

### Uber
Request a written determination covering the intended comparative view and separate bulk/history/training uses under III.G. Ask which estimate/time products and cities may be accessed, what user authorization is required, which fees/discounts are included, and which display/refresh/retention limits apply. Request the current approved technical contract after use-case acceptance. Do not equate an application key with authorization.

### Lyft
Ask whether Business/Concierge can license this consumer-facing comparison use or whether another partner agreement is needed. Request approved fare and ETA endpoints, product availability, personal-versus-business pricing distinctions, geographic limits, attribution, caching/history/training rights, minimum commitments, quotas and support expectations. Do not rely on archived consumer SDK endpoints.

### Curb
Ask for quote-only demand-partner eligibility and active fleets in Houston, Phoenix/Tempe and Santa Fe. Confirm whether taxi supply can be shown beside other authorized sources, actual fare/ETA fields, mandatory charges, expiration, coverage polygons/airports, sandbox access, fees/quotas and independent retention/forecasting rights. Ask explicitly whether comparison access obligates booking or payment integration; do not assume it is optional.

### Karhoo
Request current demand-partner requirements and production fleets for candidate cities. Confirm quote types, available/expired/complete-search semantics, currency/charges, supplier IDs, quote validity, pickup ETA and total latency. Ask whether the proposed quote-only comparative view is licensed without bookings, along with fees, quotas, attribution, redistribution, retention and forecast-training terms. Keep quote search separate from book/confirm/payment operations.

### Waymo
Ask whether a supported third-party price/pickup-ETA partnership exists for this use. If yes, request documented interface, eligibility, city/product/coverage limits and all display/history/model rights. If none is offered, keep the provider unavailable in RideForecast and retain only independently supported app handoffs; do not harvest the rider app.

### Tesla Robotaxi
Ask whether a supported rider quote/wait-time partner API exists, distinct from Fleet API and vehicle pricing. Request exact markets/geofences, service hours, eligible ride types, fare/fee/expiry semantics and licensing. Without that evidence, keep it unintegrated rather than using vehicle telemetry or app sessions.

## Review packet required before any submission/access purchase
Jaden selects provider and market, reviews the actual application text and recipient, and approves any fees or agreement separately. Record legal/commercial approval evidence privately using an opaque reference, with separate dates, scope and expiry per right. Do not place credentials, signed private agreements or personal-location records in a public API response or PR.

## Primary sources
- [U1] https://developer.uber.com/docs/riders/references/api/v1.2/estimates-price-get
- [U2] https://developer.uber.com/docs/riders/references/api/v1.2/estimates-time-get
- [U3] https://developer.uber.com/docs/riders/terms-of-use (March 2026, III.G; current terms control over old cross-reference text in endpoint docs)
- [U4] https://developer.uber.com/docs/riders/ride-requests/tutorials/deep-links/faq
- [L1] https://www.lyft.com/business/resources/ebooks/the-abcs-of-lyfts-api (official search extract available; direct retrieval returned 403)
- [L2] https://help.lyft.com/business/hc/en-us/articles/8587470351891-Managing-your-API-client-and-program-connections
- [L3] https://go.lyftbusiness.com/terms-of-service (page dated September 22, 2023; obtain the applicable current agreement and product descriptions)
- [W1] https://waymo.com/faq/
- [T1] https://www.tesla.com/support/robotaxi
- [T2] https://developer.tesla.com/docs/fleet-api/authentication/overview
- [C1] https://www.gocurb.com/curb-flow
- [C2] https://www.gocurb.com/cities
- [K1] https://karhoo.readme.io/docs/book-trips-using-apis (official docs mirror; page marked updated about six years ago, so payload/version requires confirmation)
- [K2] https://developer.karhoo.com/docs/how-to-get-access-to-the-platform

Retrieval limits: individual Karhoo quote-detail pages timed out or were inaccessible; no specific payload, TTL or rate limit was validated. Absence of a verified public API is a bounded research finding, not proof that a private commercial program cannot exist. Recheck when a provider supplies an agreement or updated documentation.

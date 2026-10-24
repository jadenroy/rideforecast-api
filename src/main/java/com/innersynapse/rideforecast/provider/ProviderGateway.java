package com.innersynapse.rideforecast.provider;

import java.time.Clock;
import java.util.List;

/** Gates upstream calls. Results are transient: no persistence, caching, training or logging. */
public final class ProviderGateway {
    private final Clock clock;
    public ProviderGateway(Clock clock) { this.clock = clock; }
    public record Result(String status, List<String> reasons, List<ProviderAdapter.Fare> fares) {}

    public Result fetch(ProviderPolicy policy, ProviderAdapter adapter, ProviderAdapter.Query query) {
        var blockers = policy.blockers(clock.instant());
        if (!blockers.isEmpty()) return unavailable(blockers);
        if (query == null || query.trip() == null) return unavailable(List.of("INVALID_TRIP"));
        var trip = query.trip();
        if (!ProviderPolicy.Geography.validPoint(trip.originLatitude(), trip.originLongitude())
                || !ProviderPolicy.Geography.validPoint(trip.destinationLatitude(), trip.destinationLongitude())
                || !Double.isFinite(trip.distanceMiles()) || trip.distanceMiles() <= 0
                || !Double.isFinite(trip.travelMinutes()) || trip.travelMinutes() <= 0) {
            return unavailable(List.of("INVALID_TRIP"));
        }
        if (!policy.coverage().contains(query.coverage())) return unavailable(List.of("UNSUPPORTED_MARKET_PRODUCT_CURRENCY"));
        var geography = query.coverage().geography();
        if (!geography.contains(trip.originLatitude(), trip.originLongitude())
                || !geography.contains(trip.destinationLatitude(), trip.destinationLongitude())) {
            return unavailable(List.of("OUTSIDE_APPROVED_GEOGRAPHY"));
        }
        if (adapter == null || !policy.id().equals(adapter.providerId())) return unavailable(List.of("ADAPTER_NOT_CONFIGURED"));
        try {
            var fares = adapter.fetch(query);
            var now = clock.instant();
            if (!policy.blockers(now).isEmpty()) return unavailable(List.of("APPROVAL_EXPIRED"));
            if (fares == null || fares.isEmpty()) return unavailable(List.of("NO_FARES"));
            for (var fare : fares) {
                if (fare == null || !policy.id().equals(fare.providerId())
                        || !query.coverage().market().equals(fare.market())
                        || !query.coverage().product().equals(fare.product())
                        || !query.coverage().currency().equals(fare.currency())
                        || fare.amount() == null || fare.amount().signum() <= 0
                        || fare.source() == null || fare.sourceReference() == null || fare.sourceReference().isBlank()
                        || fare.fetchedAt() == null || fare.fetchedAt().isAfter(now)
                        || fare.expiresAt() == null || !fare.expiresAt().isAfter(now)
                        || !fare.expiresAt().isAfter(fare.fetchedAt())) {
                    return unavailable(List.of("INVALID_OR_EXPIRED_FARE"));
                }
            }
            return new Result("AVAILABLE", List.of(), List.copyOf(fares));
        } catch (RuntimeException failure) {
            return unavailable(List.of("UPSTREAM_UNAVAILABLE"));
        }
    }

    private Result unavailable(List<String> reasons) { return new Result("UNAVAILABLE", reasons, List.of()); }
}

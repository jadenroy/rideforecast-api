package com.innersynapse.rideforecast.provider;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import static com.innersynapse.rideforecast.provider.ProviderPolicy.Capability.DOCUMENTED;
import static com.innersynapse.rideforecast.provider.ProviderPolicy.Permission.*;

@Service
public class ProviderRegistry {
    private static final Instant REVIEWED = Instant.parse("2026-10-24T00:00:00Z");
    // Research is not approval. No approved coverage or approval expiry has been established.
    private final List<ProviderPolicy> policies = List.of(
        policy("uber", "Uber", DOCUMENTED, RESTRICTED, "https://developer.uber.com/docs/riders/terms-of-use",
            "Comparison requires explicit written permission; an API key is insufficient."),
        policy("lyft", "Lyft", ProviderPolicy.Capability.UNVERIFIED, UNVERIFIED, "https://www.lyft.com/business/solutions/concierge",
            "Concierge is a business integration; consumer fare comparison access is unverified."),
        policy("curb", "Curb", ProviderPolicy.Capability.UNVERIFIED, UNVERIFIED, "https://www.gocurb.com/curb-flow",
            "Partner candidate; production access, comparison rights and coverage are unverified."),
        policy("karhoo", "Karhoo", DOCUMENTED, UNVERIFIED, "https://developer.karhoo.com/docs/book-trips-using-apis",
            "Quote API documented; production access, comparison rights and market coverage are unverified."),
        policy("waymo", "Waymo", ProviderPolicy.Capability.UNVERIFIED, UNVERIFIED, "https://waymo.com/",
            "No permitted public fare integration verified."),
        policy("tesla", "Tesla Robotaxi", ProviderPolicy.Capability.UNVERIFIED, UNVERIFIED, "https://developer.tesla.com/docs/fleet-api",
            "Fleet API documentation does not establish Robotaxi fare access.")
    );

    private static ProviderPolicy policy(String id, String name, ProviderPolicy.Capability capability,
            ProviderPolicy.Permission comparison, String url, String note) {
        return new ProviderPolicy(id, name, capability, UNVERIFIED, comparison, UNVERIFIED, UNVERIFIED,
            List.of(), new ProviderPolicy.Evidence(url, REVIEWED, null, note));
    }

    public record Availability(ProviderPolicy policy, String status, List<String> reasons) {}
    public List<Availability> availability(Instant now) {
        return policies.stream().map(policy -> {
            var reasons = new java.util.ArrayList<>(policy.blockers(now));
            reasons.add("ADAPTER_NOT_CONFIGURED");
            return new Availability(policy, "UNAVAILABLE", List.copyOf(reasons));
        }).toList();
    }
}


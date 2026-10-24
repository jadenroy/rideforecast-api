package com.innersynapse.rideforecast.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-reviewed grants. Credentials and public research links do not create these grants. */
public final class ProviderPermissionGrants {
    private ProviderPermissionGrants() {}

    public enum Right {
        PRODUCTION_ACCESS, COMPARISON, CACHING, RETENTION, REDISTRIBUTION, FORECAST_TRAINING
    }

    public record Grant(String providerId, ProviderPolicy.Permission status, String evidenceReference,
                        Instant reviewedAt, Instant validFrom, Instant validUntil, Instant revokedAt,
                        List<ProviderPolicy.Coverage> coverage) {
        public Grant { coverage = coverage == null ? List.of() : List.copyOf(coverage); }

        private boolean allows(String provider, ProviderPolicy.Coverage scope, Instant now) {
            return provider.equals(providerId) && status == ProviderPolicy.Permission.APPROVED
                && evidenceReference != null && !evidenceReference.isBlank()
                && reviewedAt != null && !reviewedAt.isAfter(now)
                && validFrom != null && !validFrom.isAfter(now)
                && validUntil != null && validUntil.isAfter(now)
                && validUntil.isAfter(validFrom)
                && (revokedAt == null || revokedAt.isAfter(now))
                && coverage.contains(scope);
        }
    }

    public static Set<Right> liveComparisonRights() {
        return Set.of(Right.PRODUCTION_ACCESS, Right.COMPARISON);
    }

    /**
     * Every requested right needs its own matching, current evidence. Callers must explicitly
     * request all rights needed by their operation. No retention/training follows from display.
     * Re-evaluate at use time and after upstream calls; do not cache this decision as authority.
     */
    public static List<String> blockers(String providerId, Map<Right, Grant> grants,
                                        Set<Right> required, ProviderPolicy.Coverage scope, Instant now) {
        if (providerId == null || providerId.isBlank() || grants == null || required == null
                || required.isEmpty() || scope == null || now == null || required.stream().anyMatch(right -> right == null)) {
            return List.of("INVALID_PERMISSION_QUERY");
        }
        var reasons = new ArrayList<String>();
        for (var right : EnumSet.copyOf(required)) {
            var grant = grants.get(right);
            if (grant == null || !grant.allows(providerId, scope, now)) {
                reasons.add("GRANT_" + right.name() + "_MISSING_INVALID_OR_EXPIRED");
            }
        }
        return List.copyOf(reasons);
    }
}

package com.innersynapse.rideforecast.provider;

import java.time.Instant;
import java.util.List;

/** Reviewed server-side policy, never supplied by a fare request or inferred from an API key. */
public record ProviderPolicy(
        String id, String name, Capability endpointCapability,
        Permission productionAccess, Permission comparisonPermission,
        Permission retentionPermission, Permission trainingPermission,
        List<Coverage> coverage, Evidence evidence) {
    public enum Capability { DOCUMENTED, UNVERIFIED }
    public enum Permission { APPROVED, UNVERIFIED, RESTRICTED }
    public record Coverage(String market, String product, String currency) {
        public Coverage {
            if (market == null || market.isBlank() || product == null || product.isBlank()
                    || currency == null || !currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("Explicit market, product and ISO currency are required");
            }
            java.util.Currency.getInstance(currency);
        }
    }
    public record Evidence(String url, Instant reviewedAt, Instant validUntil, String note) {}

    public ProviderPolicy { coverage = List.copyOf(coverage); }

    public List<String> blockers(Instant now) {
        var reasons = new java.util.ArrayList<String>();
        if (endpointCapability != Capability.DOCUMENTED) reasons.add("ENDPOINT_UNVERIFIED");
        if (productionAccess != Permission.APPROVED) reasons.add("PRODUCTION_ACCESS_UNVERIFIED");
        if (comparisonPermission != Permission.APPROVED) reasons.add("COMPARISON_NOT_APPROVED");
        if (evidence == null || evidence.reviewedAt() == null || evidence.reviewedAt().isAfter(now)
                || evidence.validUntil() == null || !evidence.validUntil().isAfter(now)
                || evidence.url() == null || evidence.url().isBlank()) reasons.add("APPROVAL_EVIDENCE_MISSING_OR_EXPIRED");
        if (coverage.isEmpty()) reasons.add("COVERAGE_UNVERIFIED");
        return List.copyOf(reasons);
    }
}

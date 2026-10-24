package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.provider.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.Instant;
import java.util.*;
import static com.innersynapse.rideforecast.provider.ProviderPermissionGrants.*;
import static org.assertj.core.api.Assertions.*;

class ProviderPermissionGrantsTests {
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");
    private final ProviderPolicy.Coverage scope = new ProviderPolicy.Coverage(
        "synthetic-market", "fixture-product", "USD", new ProviderPolicy.Geography(-1, -1, 1, 1));

    private Grant grant(String provider, Instant reviewed, Instant from, Instant until, Instant revoked,
                        String evidence, List<ProviderPolicy.Coverage> coverage) {
        return new Grant(provider, ProviderPolicy.Permission.APPROVED, evidence, reviewed, from, until, revoked, coverage);
    }
    private Grant valid() {
        return grant("fixture", now.minusSeconds(60), now.minusSeconds(60), now.plusSeconds(60), null,
            "offline-fixture-evidence", List.of(scope));
    }

    @ParameterizedTest @EnumSource(Right.class)
    void eachRightNeedsItsOwnGrant(Right right) {
        assertThat(blockers("fixture", Map.of(), Set.of(right), scope, now))
            .containsExactly("GRANT_" + right + "_MISSING_INVALID_OR_EXPIRED");
        assertThat(blockers("fixture", Map.of(right, valid()), Set.of(right), scope, now)).isEmpty();
    }

    @Test void comparisonNeverImpliesCachingRetentionRedistributionOrTraining() {
        var grants = Map.of(Right.PRODUCTION_ACCESS, valid(), Right.COMPARISON, valid());
        assertThat(blockers("fixture", grants, liveComparisonRights(), scope, now)).isEmpty();
        for (var right : Set.of(Right.CACHING, Right.RETENTION, Right.REDISTRIBUTION, Right.FORECAST_TRAINING)) {
            assertThat(blockers("fixture", grants, Set.of(right), scope, now)).hasSize(1);
        }
    }

    @Test void expiryIsIndependentAndTheBoundaryIsClosed() {
        var expired = grant("fixture", now.minusSeconds(60), now.minusSeconds(60), now, null,
            "expired-fixture", List.of(scope));
        var grants = Map.of(Right.PRODUCTION_ACCESS, valid(), Right.COMPARISON, expired, Right.RETENTION, valid());
        assertThat(blockers("fixture", grants, liveComparisonRights(), scope, now))
            .containsExactly("GRANT_COMPARISON_MISSING_INVALID_OR_EXPIRED");
        assertThat(blockers("fixture", grants, Set.of(Right.RETENTION), scope, now)).isEmpty();
    }

    @Test void unreviewedNotYetActiveRevokedAndMalformedEvidenceFailClosed() {
        var invalid = List.of(
            grant("fixture", now.plusSeconds(1), now, now.plusSeconds(60), null, "fixture", List.of(scope)),
            grant("fixture", now, now.plusSeconds(1), now.plusSeconds(60), null, "fixture", List.of(scope)),
            grant("fixture", now, now.minusSeconds(1), now.plusSeconds(60), now, "fixture", List.of(scope)),
            grant("fixture", now, now, now.plusSeconds(60), null, " ", List.of(scope)),
            grant("fixture", null, now, now.plusSeconds(60), null, "fixture", List.of(scope)),
            grant("fixture", now, null, now.plusSeconds(60), null, "fixture", List.of(scope)),
            grant("fixture", now, now, null, null, "fixture", List.of(scope)),
            grant("fixture", now, now.plusSeconds(120), now.plusSeconds(60), null, "fixture", List.of(scope)),
            grant("other-provider", now, now, now.plusSeconds(60), null, "fixture", List.of(scope)));
        for (var grant : invalid) {
            assertThat(blockers("fixture", Map.of(Right.COMPARISON, grant), Set.of(Right.COMPARISON), scope, now)).hasSize(1);
        }
    }

    @ParameterizedTest @EnumSource(value = ProviderPolicy.Permission.class, names = {"UNVERIFIED", "RESTRICTED"})
    void statusWithoutApprovalCannotAuthorizeAUse(ProviderPolicy.Permission status) {
        var g = new Grant("fixture", status, "fixture", now, now, now.plusSeconds(60), null, List.of(scope));
        assertThat(blockers("fixture", Map.of(Right.COMPARISON, g), Set.of(Right.COMPARISON), scope, now)).hasSize(1);
    }

    @Test void exactMarketProductCurrencyAndGeographyMustBeApprovedForEachRight() {
        var grants = Map.of(Right.COMPARISON, valid());
        for (var other : List.of(
                new ProviderPolicy.Coverage("other", scope.product(), "USD", scope.geography()),
                new ProviderPolicy.Coverage(scope.market(), "other", "USD", scope.geography()),
                new ProviderPolicy.Coverage(scope.market(), scope.product(), "EUR", scope.geography()),
                new ProviderPolicy.Coverage(scope.market(), scope.product(), "USD", new ProviderPolicy.Geography(-2, -2, 2, 2)))) {
            assertThat(blockers("fixture", grants, Set.of(Right.COMPARISON), other, now)).hasSize(1);
        }
    }

    @Test void grantCoverageIsAnImmutableSnapshot() {
        var mutable = new ArrayList<>(List.of(scope));
        var g = grant("fixture", now, now, now.plusSeconds(60), null, "fixture", mutable);
        mutable.clear();
        assertThat(g.coverage()).containsExactly(scope);
        assertThatThrownBy(() -> g.coverage().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void publicPolicySerializationNeverIncludesPrivateGrantReferences() {
        var g = grant("fixture", now, now, now.plusSeconds(60), null, "private-fixture-evidence", List.of(scope));
        var policy = new ProviderPolicy("fixture", "Offline fixture", ProviderPolicy.Capability.DOCUMENTED,
            ProviderPolicy.Permission.APPROVED, ProviderPolicy.Permission.APPROVED, ProviderPolicy.Permission.UNVERIFIED,
            ProviderPolicy.Permission.UNVERIFIED, List.of(scope),
            new ProviderPolicy.Evidence("https://example.test/public-research", now, now.plusSeconds(60), "Public summary"),
            Map.of(Right.PRODUCTION_ACCESS, g, Right.COMPARISON, g));
        var json = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(policy);
        assertThat(json).contains("public-research", "Offline fixture").doesNotContain("permissionGrants", "private-fixture-evidence");
    }

    @Test void invalidEvaluatorRequestsCannotReturnAnEmptySuccess() {
        assertThat(blockers("fixture", Map.of(), Set.of(), scope, now)).containsExactly("INVALID_PERMISSION_QUERY");
        assertThat(blockers("fixture", null, liveComparisonRights(), scope, now)).containsExactly("INVALID_PERMISSION_QUERY");
        assertThat(blockers("fixture", Map.of(), liveComparisonRights(), null, now)).containsExactly("INVALID_PERMISSION_QUERY");
    }
}

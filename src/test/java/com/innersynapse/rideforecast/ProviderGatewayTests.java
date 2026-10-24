package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.provider.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import static com.innersynapse.rideforecast.provider.ProviderPolicy.Capability.DOCUMENTED;
import static com.innersynapse.rideforecast.provider.ProviderPolicy.Permission.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProviderGatewayTests {
    private final Instant now = Instant.parse("2026-10-24T12:00:00Z");
    private final ProviderGateway gateway = new ProviderGateway(Clock.fixed(now, ZoneOffset.UTC));
    private final ProviderPolicy.Geography geography = new ProviderPolicy.Geography(-1, -1, 1, 1);
    private final com.innersynapse.rideforecast.dto.PriceRequest trip = new com.innersynapse.rideforecast.dto.PriceRequest(0, 0, 0.01, 0.01, "Synthetic", 1, 3, null);
    private final ProviderPolicy.Coverage scope = new ProviderPolicy.Coverage("synthetic-market", "test-product", "USD", geography);
    private final ProviderAdapter.Query query = new ProviderAdapter.Query(scope, trip);
    private final ProviderAdapter adapter = mock(ProviderAdapter.class);

    private ProviderPolicy policy(ProviderPolicy.Permission production, ProviderPolicy.Permission comparison, Instant expiry) {
        return new ProviderPolicy("fixture", "Offline fixture", DOCUMENTED, production, comparison,
            UNVERIFIED, UNVERIFIED, List.of(scope), new ProviderPolicy.Evidence("https://example.test/approval", now.minusSeconds(60), expiry, "Synthetic test only"),
            Map.of(ProviderPermissionGrants.Right.PRODUCTION_ACCESS,
                new ProviderPermissionGrants.Grant("fixture", production, "offline-production-evidence", now.minusSeconds(60), now.minusSeconds(60), expiry, null, List.of(scope)),
                ProviderPermissionGrants.Right.COMPARISON,
                new ProviderPermissionGrants.Grant("fixture", comparison, "offline-comparison-evidence", now.minusSeconds(60), now.minusSeconds(60), expiry, null, List.of(scope))));
    }
    private ProviderPolicy approved() { return policy(APPROVED, APPROVED, now.plusSeconds(600)); }
    private ProviderAdapter.Fare fare(String market, String currency, Instant fetched, Instant expires, String provenance) {
        return new ProviderAdapter.Fare("fixture", scope.product(), market, currency, new BigDecimal("12.34"),
            ProviderAdapter.Source.LICENSED_PARTNER_API, provenance, fetched, expires);
    }
    private ProviderAdapter.Fare validFare() { return fare(scope.market(), "USD", now, now.plusSeconds(60), "synthetic-fixture"); }

    @ParameterizedTest @EnumSource(value = ProviderPolicy.Permission.class, names = {"UNVERIFIED", "RESTRICTED"})
    void productionAccessIsRequiredBeforeAdapterInvocation(ProviderPolicy.Permission permission) {
        assertThat(gateway.fetch(policy(permission, APPROVED, now.plusSeconds(60)), adapter, query).fares()).isEmpty();
        verifyNoInteractions(adapter);
    }

    @ParameterizedTest @EnumSource(value = ProviderPolicy.Permission.class, names = {"UNVERIFIED", "RESTRICTED"})
    void apiAccessDoesNotGrantComparisonPermission(ProviderPolicy.Permission permission) {
        assertThat(gateway.fetch(policy(APPROVED, permission, now.plusSeconds(60)), adapter, query).reasons()).contains("COMPARISON_NOT_APPROVED");
        verifyNoInteractions(adapter);
    }

    @Test void approvedLegacySummaryWithoutIndependentGrantsCannotReachAnAdapter() {
        var legacy = new ProviderPolicy("fixture", "Offline fixture", DOCUMENTED, APPROVED, APPROVED,
            UNVERIFIED, UNVERIFIED, List.of(scope), new ProviderPolicy.Evidence("https://example.test/research", now, now.plusSeconds(600), "Research is not a grant"));
        assertThat(gateway.fetch(legacy, adapter, query).reasons())
            .containsExactly("GRANT_PRODUCTION_ACCESS_MISSING_INVALID_OR_EXPIRED", "GRANT_COMPARISON_MISSING_INVALID_OR_EXPIRED");
        verifyNoInteractions(adapter);
    }

    @Test void comparisonGrantExpiringDuringAnUpstreamCallDiscardsTheFare() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now, now.plusSeconds(2));
        var original = approved();
        var grants = new java.util.HashMap<>(original.permissionGrants());
        grants.put(ProviderPermissionGrants.Right.COMPARISON, new ProviderPermissionGrants.Grant("fixture", APPROVED,
            "offline-short-approval", now, now, now.plusSeconds(1), null, List.of(scope)));
        var policy = new ProviderPolicy(original.id(), original.name(), original.endpointCapability(), original.productionAccess(),
            original.comparisonPermission(), original.retentionPermission(), original.trainingPermission(), original.coverage(), original.evidence(), grants);
        grants.clear();
        when(adapter.providerId()).thenReturn("fixture");
        when(adapter.fetch(query)).thenReturn(List.of(validFare()));
        assertThat(new ProviderGateway(clock).fetch(policy, adapter, query).reasons())
            .containsExactly("GRANT_COMPARISON_MISSING_INVALID_OR_EXPIRED");
        verify(adapter).fetch(query);
    }

    @Test void missingOrExpiredApprovalPreventsCalls() {
        for (var policy : List.of(policy(APPROVED, APPROVED, null), policy(APPROVED, APPROVED, now))) {
            assertThat(gateway.fetch(policy, adapter, query).status()).isEqualTo("UNAVAILABLE");
        }
        verifyNoInteractions(adapter);
    }

    @Test void scopeMustMatchMarketProductAndCurrencyBeforeCalls() {
        for (var coverage : List.of(new ProviderPolicy.Coverage("other", scope.product(), "USD", geography),
                new ProviderPolicy.Coverage(scope.market(), "other", "USD", geography),
                new ProviderPolicy.Coverage(scope.market(), scope.product(), "EUR", geography))) {
            assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(coverage, trip)).reasons())
                .contains("UNSUPPORTED_MARKET_PRODUCT_CURRENCY");
        }
        verifyNoInteractions(adapter);
    }

    @Test void allProductionRegistryEntriesRemainUnavailable() {
        assertThat(new ProviderRegistry().availability(now)).hasSize(6).allSatisfy(entry -> {
            assertThat(entry.status()).isEqualTo("UNAVAILABLE");
            assertThat(entry.policy().coverage()).isEmpty();
            assertThat(gateway.fetch(entry.policy(), adapter, query).fares()).isEmpty();
        });
        verifyNoInteractions(adapter);
    }

    @Test void approvedFixtureReturnsProvenanceAndExpiryWithoutRequiringRetentionOrTraining() {
        when(adapter.providerId()).thenReturn("fixture");
        when(adapter.fetch(query)).thenReturn(List.of(validFare()));
        var result = gateway.fetch(approved(), adapter, query);
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.fares()).containsExactly(validFare());
        assertThat(approved().retentionPermission()).isEqualTo(UNVERIFIED);
        assertThat(approved().trainingPermission()).isEqualTo(UNVERIFIED);
    }

    @Test void rejectsExpiredFutureMismatchedOrUnattributedFares() {
        when(adapter.providerId()).thenReturn("fixture");
        for (var invalid : List.of(fare(scope.market(), "USD", now.minusSeconds(60), now, "fixture"),
                fare(scope.market(), "USD", now.plusSeconds(1), now.plusSeconds(60), "fixture"),
                fare("other", "USD", now, now.plusSeconds(60), "fixture"),
                fare(scope.market(), "EUR", now, now.plusSeconds(60), "fixture"),
                fare(scope.market(), "USD", now, now.plusSeconds(60), ""))) {
            when(adapter.fetch(query)).thenReturn(List.of(validFare(), invalid));
            var result = gateway.fetch(approved(), adapter, query);
            assertThat(result.reasons()).containsExactly("INVALID_OR_EXPIRED_FARE");
            assertThat(result.fares()).isEmpty();
        }
    }

    @Test void unavailableUpstreamNeverFallsBackToFormulaOrLeaksErrorDetails() {
        when(adapter.providerId()).thenReturn("fixture");
        when(adapter.fetch(query)).thenThrow(new IllegalStateException("private upstream token"));
        var result = gateway.fetch(approved(), adapter, query);
        assertThat(result.reasons()).containsExactly("UPSTREAM_UNAVAILABLE");
        assertThat(result.fares()).isEmpty();
    }

    @Test void emptyUpstreamAndMissingAdapterAreUnavailable() {
        assertThat(gateway.fetch(approved(), null, query).reasons()).containsExactly("ADAPTER_NOT_CONFIGURED");
        when(adapter.providerId()).thenReturn("fixture");
        when(adapter.fetch(query)).thenReturn(List.of());
        assertThat(gateway.fetch(approved(), adapter, query).reasons()).containsExactly("NO_FARES");
    }

    @Test void approvalExpiringDuringRequestDiscardsTheFare() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now, now.plusSeconds(600));
        when(adapter.providerId()).thenReturn("fixture");
        when(adapter.fetch(query)).thenReturn(List.of(validFare()));
        assertThat(new ProviderGateway(clock).fetch(approved(), adapter, query).reasons()).containsExactly("APPROVAL_EXPIRED");
    }

    @Test void invalidCoordinatesAndMetricsNeverReachAnAdapter() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 999, -999}) {
            var invalidTrips = List.of(
                new com.innersynapse.rideforecast.dto.PriceRequest(invalid, 0, 0, 0, "Synthetic", 1, 3, null),
                new com.innersynapse.rideforecast.dto.PriceRequest(0, invalid, 0, 0, "Synthetic", 1, 3, null),
                new com.innersynapse.rideforecast.dto.PriceRequest(0, 0, invalid, 0, "Synthetic", 1, 3, null),
                new com.innersynapse.rideforecast.dto.PriceRequest(0, 0, 0, invalid, "Synthetic", 1, 3, null));
            for (var invalidTrip : invalidTrips) {
                assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(scope, invalidTrip)).reasons()).containsExactly("INVALID_TRIP");
            }
        }
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 0, -1}) {
            var invalidTrip = new com.innersynapse.rideforecast.dto.PriceRequest(0, 0, 0.01, 0.01, "Synthetic", invalid, invalid, null);
            assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(scope, invalidTrip)).reasons()).containsExactly("INVALID_TRIP");
        }
        assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(scope, null)).reasons()).containsExactly("INVALID_TRIP");
        verifyNoInteractions(adapter);
    }

    @Test void approvedLabelsCannotAuthorizeCoordinatesOutsideApprovedTerritory() {
        for (var outside : List.of(
                new com.innersynapse.rideforecast.dto.PriceRequest(2, 0, 0, 0, "Synthetic", 1, 3, null),
                new com.innersynapse.rideforecast.dto.PriceRequest(0, 0, 0, 2, "Synthetic", 1, 3, null))) {
            assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(scope, outside)).reasons()).containsExactly("OUTSIDE_APPROVED_GEOGRAPHY");
        }
        var forged = new ProviderPolicy.Coverage(scope.market(), scope.product(), scope.currency(), new ProviderPolicy.Geography(-90, -180, 90, 180));
        assertThat(gateway.fetch(approved(), adapter, new ProviderAdapter.Query(forged, trip)).reasons()).containsExactly("UNSUPPORTED_MARKET_PRODUCT_CURRENCY");
        verifyNoInteractions(adapter);
    }

    @Test void nonMonetaryCurrencyAndInvalidGeographicScopesAreRejected() {
        for (var currency : List.of("XXX", "XTS", "XAU", "usd", "ZZZ")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ProviderPolicy.Coverage("market", "product", currency, geography))
                .isInstanceOf(IllegalArgumentException.class);
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ProviderPolicy.Geography(Double.NaN, -1, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ProviderPolicy.Geography(-1, 170, 1, -170)).isInstanceOf(IllegalArgumentException.class);
    }
}

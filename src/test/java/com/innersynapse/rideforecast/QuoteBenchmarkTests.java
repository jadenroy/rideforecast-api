package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.model.QuoteObservation;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:benchmark-policy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "rideforecast.auth.enabled=false"
})
class QuoteBenchmarkTests {
    @Autowired WebApplicationContext context;
    @Autowired QuoteObservationRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void emptyMarketHasExplicitPolicyAndNoInventedFreshnessOrPrices() throws Exception {
        mvc.perform(query("benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.dataStatus").value("NO_DATA"))
                .andExpect(jsonPath("$.minimumObservations").value(20))
                .andExpect(jsonPath("$.maxLatestObservationAgeDays").value(7))
                .andExpect(jsonPath("$.observationCount").value(0))
                .andExpect(jsonPath("$.asOf").isNotEmpty())
                .andExpect(jsonPath("$.latestObservedAt").value(nullValue()))
                .andExpect(jsonPath("$.oldestObservedAt").value(nullValue()))
                .andExpect(jsonPath("$.expectedLow").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 19})
    void inadequateSamplesNeverYieldRangesOrNormVerdicts(int count) throws Exception {
        Instant observedAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        seed(count, "USD", observedAt, 5);
        mvc.perform(query("benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.observationCount").value(count))
                .andExpect(jsonPath("$.latestObservedAt").value(observedAt.toString()))
                .andExpect(jsonPath("$.oldestObservedAt").value(observedAt.toString()))
                .andExpect(jsonPath("$.expectedLow").value(nullValue()))
                .andExpect(jsonPath("$.expectedHigh").value(nullValue()))
                .andExpect(jsonPath("$.medianPrice").value(nullValue()))
                .andExpect(jsonPath("$.averagePrice").value(nullValue()));
        mvc.perform(query("assess").param("quotedPrice", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessment").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.dataStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.observationCount").value(count))
                .andExpect(jsonPath("$.percentVsMedian").value(nullValue()));
    }

    @Test
    void twentyFreshUsdObservationsAllowDescriptiveRangeWithoutConfidenceClaim() throws Exception {
        seed(20, "USD", Instant.now().minus(6, ChronoUnit.DAYS), 5);
        mvc.perform(query("benchmark").param("distanceMiles", "5").param("currency", "usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.comparisonScope").value("MARKET_AND_SIMILAR_TRIP"))
                .andExpect(jsonPath("$.confidence").value("NOT_ESTIMATED"))
                .andExpect(jsonPath("$.expectedLow").value(14.75))
                .andExpect(jsonPath("$.medianPrice").value(19.5))
                .andExpect(jsonPath("$.expectedHigh").value(24.25));
        String[] states = {"BELOW_LOCAL_NORM", "WITHIN_LOCAL_NORM", "ABOVE_LOCAL_NORM"};
        String[] prices = {"10", "20", "30"};
        for (int i = 0; i < states.length; i++) {
            mvc.perform(query("assess").param("quotedPrice", prices[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assessment").value(states[i]))
                    .andExpect(jsonPath("$.dataStatus").value("AVAILABLE"))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }
    }

    @Test
    void staleSampleIsNotAUsableBenchmarkEvenWhenLarge() throws Exception {
        seed(20, "USD", Instant.now().minus(8, ChronoUnit.DAYS), 5);
        mvc.perform(query("benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("STALE_DATA"))
                .andExpect(jsonPath("$.observationCount").value(20))
                .andExpect(jsonPath("$.expectedLow").value(nullValue()));
        mvc.perform(query("assess").param("quotedPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("STALE_DATA"))
                .andExpect(jsonPath("$.assessment").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.percentVsMedian").value(nullValue()));
        mvc.perform(query("benchmark").param("days", "7"))
                .andExpect(jsonPath("$.dataStatus").value("NO_DATA"));
    }

    @Test
    void historicalOtherCurrenciesAndOutOfWindowOrFutureQuotesAreExcludedNotRelabeled() throws Exception {
        Instant recent = Instant.now().minus(1, ChronoUnit.DAYS);
        seed(19, "USD", recent, 5);
        seed(2, "EUR", recent, 5);
        seed(1, "USD", Instant.now().minus(31, ChronoUnit.DAYS), 5);
        seed(1, "USD", Instant.now().plus(3, ChronoUnit.MINUTES), 5);
        mvc.perform(query("benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.observationCount").value(19));
        assertThat(repository.findAll().stream().filter(q -> q.getCurrency().equals("EUR")).count()).isEqualTo(2);
        mvc.perform(get("/v1/quotes/recent").param("marketCity", "Test Market")
                        .param("marketRegion", "TS").param("marketCountry", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.currency == 'EUR')]").value(hasSize(2)));
        seed(1, "usd", recent, 5);
        mvc.perform(query("benchmark"))
                .andExpect(jsonPath("$.dataStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.observationCount").value(20));
    }

    @Test
    void similarTripsNeedTheSameMinimumBeforeReplacingTheCityWideSample() throws Exception {
        Instant recent = Instant.now().minus(1, ChronoUnit.DAYS);
        seed(19, "USD", recent, 5);
        seed(1, "USD", recent, 50);
        mvc.perform(query("benchmark").param("distanceMiles", "5"))
                .andExpect(jsonPath("$.comparisonScope").value("MARKET_ONLY_FALLBACK"))
                .andExpect(jsonPath("$.observationCount").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EUR", "CAD", "ZZZ"})
    void betaRejectsOtherCurrenciesForWritesAndBothComparisonEndpoints(String currency) throws Exception {
        mvc.perform(post("/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(payload(currency)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currency").value("must be USD during the beta"));
        mvc.perform(query("benchmark").param("currency", currency)).andExpect(status().isBadRequest());
        mvc.perform(query("assess").param("quotedPrice", "20").param("currency", currency))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void lowercaseUsdIsStoredAsUsdAndRemainsIdempotent() throws Exception {
        mvc.perform(post("/v1/quotes").header("Idempotency-Key", "usd-policy-test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(payload("usd")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.currency").value("USD"));
        mvc.perform(post("/v1/quotes").header("Idempotency-Key", "usd-policy-test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(payload("usd")))
                .andExpect(status().isOk()).andExpect(header().string("Idempotency-Replayed", "true"));
        assertThat(repository.count()).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder query(String endpoint) {
        return get("/v1/quotes/" + endpoint).param("marketCity", "Test Market").param("marketRegion", "TS")
                .param("marketCountry", "US").param("provider", "Uber").param("rideType", "UberX");
    }

    private void seed(int count, String currency, Instant observedAt, double miles) {
        for (int i = 0; i < count; i++) {
            repository.save(new QuoteObservation(UUID.randomUUID(), "Uber", "UberX", 10 + i, currency,
                    0, 0, 0.01, 0.01, "Test Market", "TS", "US", "us|ts|test-market", null, null,
                    miles, 15, observedAt, Instant.now(), "synthetic-test", null, null, null));
        }
    }

    private String payload(String currency) {
        return """
                {"provider":"Uber","rideType":"UberX","quotedPrice":20,"currency":"%s",
                 "originLatitude":0,"originLongitude":0,"destinationLatitude":0.01,"destinationLongitude":0.01,
                 "marketCity":"Test Market","marketRegion":"TS","marketCountry":"US",
                 "roadDistanceMiles":5,"travelMinutes":15,"observedAt":"2026-01-01T00:00:00Z"}
                """.formatted(currency);
    }
}

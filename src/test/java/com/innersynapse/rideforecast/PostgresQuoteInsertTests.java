package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.model.QuoteObservation;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "rf027.postgres.required", matches = "true")
class PostgresQuoteInsertTests {
    @Autowired DataSource dataSource;
    @Autowired QuoteObservationRepository repository;

    @BeforeEach
    void requireActualPostgresFixture() throws Exception {
        PostgresTestDatabase.assertRequiredDatabase(dataSource);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void everyInsertedFieldAndMicrosecondInstantRoundTrips(boolean nullableFields) {
        String key = "synthetic-roundtrip-" + UUID.randomUUID();
        var observation = new QuoteObservation(UUID.randomUUID(), "Synthetic Å Provider", "Standard", 37.25,
                "USD", 1.234, -145.678, -2.345, 146.789, "Synthetic City", "TS", "US", "synthetic|ts|us",
                nullableFields ? null : "Origin Zone", nullableFields ? null : "Destination Zone", 7.125, 23.75,
                OffsetDateTime.parse("2026-01-01T12:34:56.123456+05:45").toInstant(),
                OffsetDateTime.parse("2026-01-02T03:04:05.654321-07:00").toInstant(), "synthetic-source", key,
                nullableFields ? null : "a".repeat(64),
                nullableFields ? null : UUID.fromString("00000000-0000-0000-0000-000000000003"));
        try {
            repository.insert(observation);
            var stored = repository.findById(observation.getId()).orElseThrow();
            assertThat(stored).usingRecursiveComparison().isEqualTo(observation);
            assertThat(repository.findBySubmissionKey(key)).isPresent();
        } finally {
            repository.findBySubmissionKey(key).ifPresent(repository::delete);
        }
    }
}

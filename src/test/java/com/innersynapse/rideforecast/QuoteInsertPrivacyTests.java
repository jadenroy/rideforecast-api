package com.innersynapse.rideforecast;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.innersynapse.rideforecast.model.QuoteObservation;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import com.innersynapse.rideforecast.dto.QuoteObservationRequest;
import com.innersynapse.rideforecast.service.QuoteObservationService;
import com.innersynapse.rideforecast.exception.InvalidQuoteSubmissionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class QuoteInsertPrivacyTests {
    @Autowired QuoteObservationRepository repository;

    @ParameterizedTest
    @CsvSource({"same,false", "same,true", "different,false", "account-guest,false", "guest-account,false"})
    void serviceRaceStillChecksOwnerAndPayload(String owners, boolean changedPayload) throws Exception {
        String key = "synthetic-service-race-" + UUID.randomUUID();
        UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID firstOwner = owners.equals("guest-account") ? null : alice;
        UUID secondOwner = owners.equals("account-guest") ? null : owners.equals("different") ? bob : alice;
        var barrier = new CountDownLatch(2);
        var lookups = new AtomicInteger();
        var delegated = org.mockito.Mockito.mock(QuoteObservationRepository.class,
                org.mockito.AdditionalAnswers.delegatesTo(repository));
        org.mockito.Mockito.doAnswer(call -> {
            var found = repository.findBySubmissionKey(key);
            if (lookups.incrementAndGet() <= 2) {
                assertThat(found).isEmpty();
                barrier.countDown();
                assertThat(barrier.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return found;
        }).when(delegated).findBySubmissionKey(key);
        var service = new QuoteObservationService(delegated);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> saveOutcome(service, payload(15), key, firstOwner));
            var second = executor.submit(() -> saveOutcome(service, payload(changedPayload ? 16 : 15), key, secondOwner));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("created", owners.equals("same") && !changedPayload ? "replayed" : "conflict");
            assertThat(repository.findBySubmissionKey(key)).isPresent();
        } finally {
            repository.findBySubmissionKey(key).ifPresent(repository::delete);
        }
    }

    private String saveOutcome(QuoteObservationService service, QuoteObservationRequest payload, String key, UUID owner) {
        try {
            return service.save(payload, key, owner).replayed() ? "replayed" : "created";
        } catch (InvalidQuoteSubmissionException conflict) {
            assertThat(conflict.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
            assertThat(conflict.getMessage()).isEqualTo("This submission key cannot be reused for this request.");
            return "conflict";
        }
    }

    private QuoteObservationRequest payload(double price) {
        return new QuoteObservationRequest("SyntheticProvider", "Standard", price, "USD", 35.1, -106.1,
                35.2, -106.2, "Synthetic City", "NM", "US", "Zone A", "Zone B", 6, 18,
                Instant.parse("2026-01-01T00:00:00Z"), "synthetic-test", "");
    }

    @Test
    void concurrentInsertKeepsKeyOutOfLogsAndRetainsUsefulDiagnostics() throws Exception {
        String key = "synthetic-private-key-" + UUID.randomUUID();
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        var capture = new ListAppender<ILoggingEvent>();
        capture.list = new CopyOnWriteArrayList<>();
        capture.start();
        root.addAppender(capture);
        try {
            var start = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> insertAfter(start, quote(key, "SyntheticProvider")));
                var second = executor.submit(() -> insertAfter(start, quote(key, "SyntheticProvider")));
                start.countDown();
                assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(true, false);
            }
            assertThat(repository.findBySubmissionKey(key)).isPresent();
            LoggerFactory.getLogger("org.hibernate.orm.jdbc.error").warn("synthetic-unrelated-diagnostic");
            assertThat(capture.list).extracting(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.equals("QUOTE_INSERT_UNIQUE_CONFLICT sqlState=23505"))
                    .anyMatch(message -> message.equals("synthetic-unrelated-diagnostic"))
                    .noneMatch(message -> message.contains(key));
            assertThat(capture.list).allMatch(event -> event.getThrowableProxy() == null);
        } finally {
            root.detachAppender(capture);
            capture.stop();
            repository.findBySubmissionKey(key).ifPresent(repository::delete);
        }
    }

    @Test
    void unrelatedIntegrityFailureStillCarriesItsDriverDiagnostic() {
        assertThatThrownBy(() -> repository.insert(quote("synthetic-invalid-" + UUID.randomUUID(), null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(java.sql.SQLException.class)
                .satisfies(error -> assertThat(((java.sql.SQLException) error.getCause()).getSQLState())
                        .isEqualTo("23502"));
    }

    private boolean insertAfter(CountDownLatch start, QuoteObservation q) throws Exception {
        start.await();
        try {
            repository.insert(q);
            return true;
        } catch (DataIntegrityViolationException conflict) {
            assertThat(conflict).hasMessage("Quote insert unique conflict (SQLSTATE 23505)").hasNoCause();
            return false;
        }
    }

    private QuoteObservation quote(String key, String provider) {
        return new QuoteObservation(UUID.randomUUID(), provider, "Standard", 15.0, "USD",
                35.1, -106.1, 35.2, -106.2, "Synthetic City", "NM", "US", "synthetic|nm|us",
                "Zone A", "Zone B", 6, 18, Instant.now(), Instant.now(), "synthetic-test", key,
                "synthetic-fingerprint", null);
    }
}

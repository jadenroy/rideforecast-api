package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.dto.QuoteObservationRequest;
import com.innersynapse.rideforecast.exception.InvalidQuoteSubmissionException;
import com.innersynapse.rideforecast.model.QuoteObservation;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import com.innersynapse.rideforecast.service.QuoteObservationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuoteReplayOwnershipTests {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @ParameterizedTest
    @CsvSource({"alice,bob", "alice,guest", "guest,alice"})
    void rejectsCrossOwnerReplayOnBothLookupPaths(String original, String retry) {
        for (boolean concurrent : new boolean[]{false, true}) {
            for (String key : new String[]{"synthetic-replay-key", null}) {
                var repository = mock(QuoteObservationRepository.class);
                var service = new QuoteObservationService(repository);
                when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
                service.save(payload(), key, owner(original));
                var captor = org.mockito.ArgumentCaptor.forClass(QuoteObservation.class);
                verify(repository).saveAndFlush(captor.capture());
                var stored = captor.getValue();
                reset(repository);
                if (concurrent) {
                    when(repository.findBySubmissionKey(any())).thenReturn(Optional.empty(), Optional.of(stored));
                    when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("synthetic conflict"));
                } else {
                    when(repository.findBySubmissionKey(any())).thenReturn(Optional.of(stored));
                }
                assertThatThrownBy(() -> service.save(payload(), key, owner(retry)))
                        .isInstanceOfSatisfying(InvalidQuoteSubmissionException.class,
                                error -> assertThat(error.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
                assertThat(stored.getOwnerId()).isEqualTo(owner(original));
                if (!concurrent) verify(repository, never()).saveAndFlush(any());
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"alice", "guest"})
    void preservesSameOwnerRetries(String original) {
        for (String key : new String[]{"synthetic-replay-key", null}) {
            var repository = mock(QuoteObservationRepository.class);
            var service = new QuoteObservationService(repository);
            when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
            var first = service.save(payload(), key, owner(original));
            var captor = org.mockito.ArgumentCaptor.forClass(QuoteObservation.class);
            verify(repository).saveAndFlush(captor.capture());
            when(repository.findBySubmissionKey(any())).thenReturn(Optional.of(captor.getValue()));
            var replay = service.save(payload(), key, owner(original));
            assertThat(replay.replayed()).isTrue();
            assertThat(replay.observation()).isEqualTo(first.observation());
            verify(repository, times(1)).saveAndFlush(any());
        }
    }

    private UUID owner(String name) { return name.equals("guest") ? null : name.equals("alice") ? ALICE : BOB; }

    private QuoteObservationRequest payload() {
        return new QuoteObservationRequest("Uber", "UberX", 16.95, "USD", 35.08, -106.65,
                35.12, -106.57, "Synthetic City", "NM", "US", "Zone A", "Zone B", 6.2, 18,
                Instant.parse("2026-01-01T00:00:00Z"), "synthetic-test", "");
    }
}

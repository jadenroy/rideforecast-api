package com.innersynapse.rideforecast.repository;

import com.innersynapse.rideforecast.model.QuoteObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteObservationRepository extends JpaRepository<QuoteObservation, UUID>, QuoteObservationInsert {

    List<QuoteObservation> findByMarketKeyAndProviderIgnoreCaseAndRideTypeIgnoreCaseAndCurrencyIgnoreCaseAndObservedAtBetweenOrderByObservedAtDesc(
            String marketKey,
            String provider,
            String rideType,
            String currency,
            Instant cutoff,
            Instant asOf
    );

    List<QuoteObservation> findTop100ByMarketKeyOrderByObservedAtDesc(String marketKey);

    Optional<QuoteObservation> findBySubmissionKey(String submissionKey);

    List<QuoteObservation> findTop100ByOwnerIdOrderByObservedAtDesc(UUID ownerId);

    void deleteByOwnerId(UUID ownerId);
}

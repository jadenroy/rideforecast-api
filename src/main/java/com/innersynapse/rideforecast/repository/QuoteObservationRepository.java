package com.innersynapse.rideforecast.repository;

import com.innersynapse.rideforecast.model.QuoteObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuoteObservationRepository extends JpaRepository<QuoteObservation, UUID> {

    List<QuoteObservation> findByMarketKeyAndProviderIgnoreCaseAndRideTypeIgnoreCaseOrderByObservedAtDesc(
            String marketKey,
            String provider,
            String rideType
    );

    List<QuoteObservation> findTop100ByMarketKeyOrderByObservedAtDesc(String marketKey);
}

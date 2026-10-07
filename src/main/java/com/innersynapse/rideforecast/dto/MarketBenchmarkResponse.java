package com.innersynapse.rideforecast.dto;

import java.time.Instant;

public record MarketBenchmarkResponse(
        String marketCity,
        String marketRegion,
        String marketCountry,
        String marketKey,
        String provider,
        String rideType,
        int days,
        long observationCount,
        Double requestedDistanceMiles,
        Double requestedTravelMinutes,
        String comparisonScope,
        Double minimumPrice,
        Double expectedLow,
        Double averagePrice,
        Double medianPrice,
        Double expectedHigh,
        Double maximumPrice,
        String confidence,
        String currency,
        String dataStatus,
        int minimumObservations,
        int maxLatestObservationAgeDays,
        Instant asOf,
        Instant oldestObservedAt,
        Instant latestObservedAt
) {
}

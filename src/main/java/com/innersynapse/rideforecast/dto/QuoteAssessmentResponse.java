package com.innersynapse.rideforecast.dto;

import java.time.Instant;

public record QuoteAssessmentResponse(
        String provider,
        String rideType,
        double quotedPrice,
        String marketKey,
        long observationCount,
        Double expectedLow,
        Double medianPrice,
        Double expectedHigh,
        Double percentVsMedian,
        String assessment,
        String confidence,
        String comparisonScope,
        String currency,
        String dataStatus,
        int minimumObservations,
        int maxLatestObservationAgeDays,
        Instant asOf,
        Instant oldestObservedAt,
        Instant latestObservedAt
) {
}

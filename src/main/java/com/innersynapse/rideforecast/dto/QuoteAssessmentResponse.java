package com.innersynapse.rideforecast.dto;

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
        String comparisonScope
) {
}

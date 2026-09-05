package com.innersynapse.rideforecast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record QuoteObservationRequest(
        @NotBlank String provider,
        @NotBlank String rideType,
        @Positive double quotedPrice,
        @NotBlank String currency,
        @NotNull Double originLatitude,
        @NotNull Double originLongitude,
        @NotNull Double destinationLatitude,
        @NotNull Double destinationLongitude,
        @NotBlank String marketCity,
        @NotBlank String marketRegion,
        @NotBlank String marketCountry,
        String originZone,
        String destinationZone,
        @Positive double roadDistanceMiles,
        @Positive double travelMinutes,
        Instant observedAt,
        String source
) {
}

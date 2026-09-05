package com.innersynapse.rideforecast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PriceRequest(
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude,
        @NotBlank String destinationLabel,
        @Positive double distanceMiles,
        @Positive double travelMinutes,
        String requestedAt
) {}

package com.innersynapse.rideforecast.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record QuoteObservationRequest(
        @NotBlank
        @Pattern(regexp = "(?i)Uber|Lyft|Waymo|Tesla", message = "must be Uber, Lyft, Waymo, or Tesla")
        String provider,
        @NotBlank @Size(max = 80) String rideType,
        @DecimalMin(value = "0.01") @DecimalMax(value = "10000.00") double quotedPrice,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "must be a three-letter currency code")
        String currency,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double originLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double originLongitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double destinationLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double destinationLongitude,
        @NotBlank @Size(max = 120) String marketCity,
        @NotBlank @Size(max = 120) String marketRegion,
        @NotBlank @Size(max = 80) String marketCountry,
        @Size(max = 120) String originZone,
        @Size(max = 120) String destinationZone,
        @DecimalMin(value = "0.01") @DecimalMax(value = "1000.00") double roadDistanceMiles,
        @DecimalMin(value = "0.1") @DecimalMax(value = "1440.0") double travelMinutes,
        Instant observedAt,
        @Size(max = 80) String source,
        @Size(max = 200) String website
) {
}

package com.innersynapse.rideforecast.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String preferredName,
        String homeCity,
        String homeRegion,
        String homeCountry,
        String termsVersion,
        Instant termsAcceptedAt,
        String privacyVersion,
        Instant privacyAcceptedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

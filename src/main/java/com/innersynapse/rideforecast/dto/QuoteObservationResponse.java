package com.innersynapse.rideforecast.dto;

import java.time.Instant;
import java.util.UUID;

public record QuoteObservationResponse(
        UUID id,
        String provider,
        String rideType,
        double quotedPrice,
        String currency,
        String marketCity,
        String marketRegion,
        String marketCountry,
        String marketKey,
        String originZone,
        String destinationZone,
        double roadDistanceMiles,
        double travelMinutes,
        Instant observedAt,
        String source
) {
}

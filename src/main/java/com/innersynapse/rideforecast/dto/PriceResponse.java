package com.innersynapse.rideforecast.dto;

import java.time.Instant;
import java.util.List;

public record PriceResponse(
        String source,
        Instant fetchedAt,
        List<RidePriceDto> rides
) {}

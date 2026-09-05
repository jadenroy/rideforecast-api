package com.innersynapse.rideforecast.dto;

public record RidePriceDto(
        String provider,
        String rideType,
        double price
) {}

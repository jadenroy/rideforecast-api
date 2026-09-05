package com.innersynapse.rideforecast.service;

import com.innersynapse.rideforecast.dto.PriceRequest;
import com.innersynapse.rideforecast.dto.PriceResponse;
import com.innersynapse.rideforecast.dto.RidePriceDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PriceService {

    public PriceResponse getPrices(PriceRequest request) {
        double demand = demandMultiplier();

        double uberBase = calculateUberBase(
                request.distanceMiles(),
                request.travelMinutes(),
                demand
        );

        double lyftBase = calculateLyftBase(
                request.distanceMiles(),
                request.travelMinutes(),
                demand
        );

        List<RidePriceDto> rides = List.of(
                new RidePriceDto("Uber", "UberX", money(uberBase)),
                new RidePriceDto("Uber", "Comfort", money(uberBase * 1.38)),
                new RidePriceDto("Uber", "UberXL", money(uberBase * 1.72)),
                new RidePriceDto("Lyft", "Standard", money(lyftBase)),
                new RidePriceDto("Lyft", "Lyft XL", money(lyftBase * 1.50)),
                new RidePriceDto("Lyft", "Lux", money(lyftBase * 2.05))
        );

        return new PriceResponse(
                "innersynapse-simulation",
                Instant.now(),
                rides
        );
    }

    private double calculateUberBase(double distanceMiles, double travelMinutes, double demand) {
        double booking = 3.75;
        double mileage = distanceMiles * 1.42;
        double time = travelMinutes * 0.24;
        return Math.max(7.50, (booking + mileage + time) * demand);
    }

    private double calculateLyftBase(double distanceMiles, double travelMinutes, double demand) {
        double booking = 3.55;
        double mileage = distanceMiles * 1.38;
        double time = travelMinutes * 0.23;
        return Math.max(7.25, (booking + mileage + time) * demand * 0.98);
    }

    private double demandMultiplier() {
        int hour = ZonedDateTime.now(ZoneId.systemDefault()).getHour();

        if (hour >= 6 && hour < 9) return 1.28;
        if (hour >= 9 && hour < 16) return 1.05;
        if (hour >= 16 && hour < 19) return 1.34;
        if (hour >= 19 && hour < 21) return 1.18;
        if (hour >= 21 && hour < 23) return 1.36;
        if (hour == 23) return 1.58;
        if (hour == 0) return 1.72;
        if (hour == 1) return 1.88;
        if (hour == 2) return 2.20;
        if (hour == 3) return 1.42;
        return 1.05;
    }

    private double money(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

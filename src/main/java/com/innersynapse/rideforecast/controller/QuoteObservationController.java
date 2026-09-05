package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.dto.MarketBenchmarkResponse;
import com.innersynapse.rideforecast.dto.QuoteObservationRequest;
import com.innersynapse.rideforecast.dto.QuoteObservationResponse;
import com.innersynapse.rideforecast.service.QuoteObservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/quotes")
@Validated
public class QuoteObservationController {

    private final QuoteObservationService service;

    public QuoteObservationController(QuoteObservationService service) {
        this.service = service;
    }

    @PostMapping
    public QuoteObservationResponse create(
            @Valid @RequestBody QuoteObservationRequest request
    ) {
        return service.save(request);
    }

    @GetMapping("/recent")
    public List<QuoteObservationResponse> recent(
            @RequestParam String marketCity,
            @RequestParam String marketRegion,
            @RequestParam String marketCountry
    ) {
        return service.recent(marketCity, marketRegion, marketCountry);
    }

    @GetMapping("/benchmark")
    public MarketBenchmarkResponse benchmark(
            @RequestParam String marketCity,
            @RequestParam String marketRegion,
            @RequestParam String marketCountry,
            @RequestParam String provider,
            @RequestParam String rideType,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        return service.benchmark(
                marketCity,
                marketRegion,
                marketCountry,
                provider,
                rideType,
                days
        );
    }
}

package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.dto.PriceRequest;
import com.innersynapse.rideforecast.dto.PriceResponse;
import com.innersynapse.rideforecast.service.PriceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @PostMapping
    public PriceResponse getPrices(@Valid @RequestBody PriceRequest request) {
        return priceService.getPrices(request);
    }
}

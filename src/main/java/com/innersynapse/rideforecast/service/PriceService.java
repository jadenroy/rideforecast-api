package com.innersynapse.rideforecast.service;

import com.innersynapse.rideforecast.dto.PriceRequest;
import com.innersynapse.rideforecast.dto.PriceResponse;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class PriceService {
    public PriceResponse getPrices(PriceRequest request) {
        // Legacy payload lacks market/currency/product scope. Never invent fares or infer permissions.
        return new PriceResponse("unavailable", Instant.now(), List.of(), "UNAVAILABLE",
            "NO_APPROVED_PROVIDER_INTEGRATION", "/v1/providers");
    }
}

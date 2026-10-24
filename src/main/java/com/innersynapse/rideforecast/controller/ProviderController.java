package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.provider.ProviderRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
public class ProviderController {
    private final ProviderRegistry registry;
    public ProviderController(ProviderRegistry registry) { this.registry = registry; }
    public record Response(String status, Instant checkedAt, boolean exhaustive,
                           List<ProviderRegistry.Availability> providers) {}

    @GetMapping("/v1/providers")
    public ResponseEntity<Response> providers() {
        var now = Instant.now();
        return ResponseEntity.ok().header("Cache-Control", "no-store")
            .body(new Response("UNAVAILABLE", now, false, registry.availability(now)));
    }
}

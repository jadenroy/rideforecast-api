package com.innersynapse.rideforecast.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import javax.sql.DataSource;
import java.sql.SQLException;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Keep the existing Render probe and the browser probe on the same readiness check.
    @GetMapping({"/health", "/v1/ready"})
    public ResponseEntity<Map<String, Object>> health() {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.setQueryTimeout(2);
            // An empty table is healthy; a missing table or unavailable database is not.
            try (var ignored = statement.executeQuery("SELECT 1 FROM quote_observations LIMIT 1")) {
                return ResponseEntity.ok().header("Cache-Control", "no-store").body(Map.of(
                "status", "ok",
                "database", "ready",
                "service", "rideforecast-api",
                "timestamp", Instant.now().toString()
                ));
            }
        } catch (SQLException | RuntimeException failure) {
            return ResponseEntity.status(503).header("Cache-Control", "no-store")
                    .header("Retry-After", "5").body(Map.of("status", "unavailable", "service", "rideforecast-api"));
        }
    }
}

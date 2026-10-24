package com.innersynapse.rideforecast;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
class ProviderAvailabilityTests {
    @Autowired WebApplicationContext context;

    @Test void availabilityIsPublicNoStoreAndDoesNotClaimCoverage() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(get("/v1/providers").header("Origin", "https://rideforecast.app"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Access-Control-Allow-Origin", "https://rideforecast.app"))
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.exhaustive").value(false))
            .andExpect(jsonPath("$.providers", hasSize(6)))
            .andExpect(jsonPath("$.providers[*].status", everyItem(is("UNAVAILABLE"))))
            .andExpect(jsonPath("$.providers[0].policy.comparisonPermission").value("RESTRICTED"))
            .andExpect(jsonPath("$.providers[0].policy.coverage", hasSize(0)));
    }

    @Test void legacyPriceRouteReturnsNoSyntheticFaresAndKeepsEnvelope() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(post("/v1/prices").contentType("application/json").content("""
                {"originLatitude":0,"originLongitude":0,"destinationLatitude":0.01,
                 "destinationLongitude":0.01,"destinationLabel":"Synthetic test location",
                 "distanceMiles":1,"travelMinutes":3}
                """))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.rides", hasSize(0)))
            .andExpect(jsonPath("$.source").value("unavailable"))
            .andExpect(jsonPath("$.fetchedAt").exists())
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.reason").value("NO_APPROVED_PROVIDER_INTEGRATION"))
            .andExpect(jsonPath("$.availabilityUrl").value("/v1/providers"));
    }
}

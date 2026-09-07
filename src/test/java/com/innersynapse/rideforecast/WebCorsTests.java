package com.innersynapse.rideforecast;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class WebCorsTests {
    @Autowired WebApplicationContext context;

    @Test
    void allowsQuoteSubmissionFromBrandedProductionWeb() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(options("/v1/quotes")
                .header("Origin", "https://rideforecast.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,idempotency-key"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://rideforecast.app"))
            .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsStringIgnoringCase("idempotency-key")));
    }

    @Test
    void keepsInnerSynapseDomainCompatible() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(options("/v1/quotes")
                .header("Origin", "https://rideforecast.innersynapse.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://rideforecast.innersynapse.com"));
    }

    @Test
    void rejectsUnconfiguredBrowserOrigins() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(options("/v1/quotes")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }
}

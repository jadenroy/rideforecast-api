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
    void allowsQuoteSubmissionFromProductionWeb() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build()
            .perform(options("/v1/quotes")
                .header("Origin", "https://rideforecast.innersynapse.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
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

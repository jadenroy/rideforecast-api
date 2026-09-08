package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.config.SecurityHeadersFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityHeadersTests {

    @Autowired WebApplicationContext context;
    @Autowired SecurityHeadersFilter securityHeadersFilter;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityHeadersFilter)
                .build();
    }

    @Test
    void appliesBrowserSecurityHeadersToApiResponses() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"));
    }

    @Test
    void preventsPrivateProfileResponsesFromBeingCached() throws Exception {
        mvc.perform(get("/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}

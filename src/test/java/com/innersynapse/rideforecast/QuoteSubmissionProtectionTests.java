package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import com.innersynapse.rideforecast.config.QuoteSubmissionRateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "rideforecast.submissions.max-per-minute=3")
class QuoteSubmissionProtectionTests {

    @Autowired WebApplicationContext context;
    @Autowired QuoteObservationRepository repository;
    @Autowired QuoteSubmissionRateLimitFilter rateLimitFilter;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(rateLimitFilter)
                .build();
    }

    @Test
    void createsOnceAndReplaysTheSameIdempotentSubmission() throws Exception {
        String body = validPayload(16.95, "");
        String first = mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.10"))
                        .header("Idempotency-Key", "quote-test-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.10"))
                        .header("Idempotency-Key", "quote-test-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(content().json(first));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void fingerprintsIdenticalLegacyRetriesWithoutAHeader() throws Exception {
        String body = validPayload(18.25, "");

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void rejectsReuseOfAKeyForDifferentQuoteDetails() throws Exception {
        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.30"))
                        .header("Idempotency-Key", "quote-test-key-0003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(12.00, "")))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.30"))
                        .header("Idempotency-Key", "quote-test-key-0003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(13.00, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void rejectsImpossibleNumbersFutureTimesAndTheBotTrap() throws Exception {
        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.40"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10000.01, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUOTE"))
                .andExpect(jsonPath("$.fieldErrors.quotedPrice").exists());

        String future = validPayload(
                15.00,
                "",
                Instant.now().plus(10, ChronoUnit.MINUTES)
        );
        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.41"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(future))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FUTURE_OBSERVATION"));

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.42"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(15.00, "https://spam.example")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTOMATED_SUBMISSION_REJECTED"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void limitsEverySubmissionAttemptBeforeItCanReachValidation() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            mvc.perform(post("/v1/quotes")
                            .with(request -> remote(request, "203.0.113.50"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.50"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "3"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void databaseUniquenessProtectsSimultaneousDuplicateWrites() throws Exception {
        String body = validPayload(21.40, "");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = List.of(
                    executor.submit(() -> submitAfter(start, body)),
                    executor.submit(() -> submitAfter(start, body))
            );
            start.countDown();

            assertThat(List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(201, 200);
        }
        assertThat(repository.count()).isEqualTo(1);
    }

    private int submitAfter(CountDownLatch start, String body) throws Exception {
        start.await();
        return mvc.perform(post("/v1/quotes")
                        .with(request -> remote(request, "203.0.113.60"))
                        .header("Idempotency-Key", "quote-test-key-concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    private org.springframework.mock.web.MockHttpServletRequest remote(
            org.springframework.mock.web.MockHttpServletRequest request,
            String address
    ) {
        request.setRemoteAddr(address);
        return request;
    }

    private String validPayload(double price, String website) {
        return validPayload(price, website, Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    private String validPayload(double price, String website, Instant observedAt) {
        return """
                {
                  "provider":"Uber",
                  "rideType":"UberX",
                  "quotedPrice":%s,
                  "currency":"USD",
                  "originLatitude":35.6878467,
                  "originLongitude":-105.9388933,
                  "destinationLatitude":35.6406403,
                  "destinationLongitude":-106.010598,
                  "marketCity":"Santa Fe",
                  "marketRegion":"NM",
                  "marketCountry":"US",
                  "originZone":"Downtown Santa Fe",
                  "destinationZone":"Southwest",
                  "roadDistanceMiles":5.9,
                  "travelMinutes":20,
                  "observedAt":"%s",
                  "source":"user-observed-web",
                  "website":"%s"
                }
                """.formatted(price, observedAt.truncatedTo(ChronoUnit.SECONDS), website);
    }
}

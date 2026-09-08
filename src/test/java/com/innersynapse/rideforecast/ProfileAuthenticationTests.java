package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.auth.FirebaseAuthenticationFilter;
import com.innersynapse.rideforecast.auth.FirebaseTokenVerifier;
import com.innersynapse.rideforecast.auth.InvalidAuthenticationException;
import com.innersynapse.rideforecast.auth.VerifiedIdentity;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import com.innersynapse.rideforecast.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rideforecast.auth.enabled=true",
        "rideforecast.auth.allow-anonymous-quotes=false"
})
@Import(ProfileAuthenticationTests.TestAuthConfiguration.class)
class ProfileAuthenticationTests {

    @Autowired WebApplicationContext context;
    @Autowired FirebaseAuthenticationFilter authenticationFilter;
    @Autowired UserProfileRepository profiles;
    @Autowired QuoteObservationRepository quotes;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        quotes.deleteAll();
        profiles.deleteAll();
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(authenticationFilter)
                .build();
    }

    @Test
    void privateProfileEndpointsRequireAValidToken() throws Exception {
        mvc.perform(get("/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mvc.perform(get("/v1/profile").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_AUTH_TOKEN"));

        mvc.perform(get("/v1/profile").header("Authorization", "Bearer unsafe-message-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_AUTH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Signed out \"now\".\nRetry."));

        mvc.perform(get("/v1/profile").header("Authorization", "Bearer jaden-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void createsUpdatesAndReadsOnlyTheAuthenticatedProfile() throws Exception {
        mvc.perform(put("/v1/profile")
                        .header("Authorization", "Bearer jaden-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("Jaden Hylton", "Santa Fe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredName").value("Jaden Hylton"))
                .andExpect(jsonPath("$.homeCity").value("Santa Fe"))
                .andExpect(jsonPath("$.termsVersion").value("2026-09-08"));

        mvc.perform(get("/v1/profile").header("Authorization", "Bearer jaden-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredName").value("Jaden Hylton"));

        mvc.perform(get("/v1/profile").header("Authorization", "Bearer other-token"))
                .andExpect(status().isNotFound());

        assertThat(profiles.count()).isEqualTo(1);
    }

    @Test
    void validatesConsentAndProfileFields() throws Exception {
        mvc.perform(put("/v1/profile")
                        .header("Authorization", "Bearer jaden-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preferredName":"","homeCountry":"USA","termsVersion":"2026-09-08",
                                 "privacyVersion":"2026-09-08","acceptTerms":false,"acceptPrivacy":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROFILE"))
                .andExpect(jsonPath("$.fieldErrors.preferredName").exists())
                .andExpect(jsonPath("$.fieldErrors.homeCountry").exists())
                .andExpect(jsonPath("$.fieldErrors.acceptTerms").exists())
                .andExpect(jsonPath("$.fieldErrors.acceptPrivacy").exists());
    }

    @Test
    void attachesQuotesToAProfileAndDeletesAllOwnedData() throws Exception {
        mvc.perform(post("/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mvc.perform(post("/v1/quotes")
                        .header("Authorization", "Bearer jaden-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_REQUIRED"));

        mvc.perform(put("/v1/profile")
                        .header("Authorization", "Bearer jaden-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("Jaden", "Albuquerque")))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/quotes")
                        .header("Authorization", "Bearer jaden-token")
                        .header("Idempotency-Key", "profile-quote-test-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody()))
                .andExpect(status().isCreated());

        assertThat(quotes.findAll()).singleElement().satisfies(quote -> {
            assertThat(quote.getOwnerId()).isNotNull();
            assertThat(quote.getOriginLatitude()).isEqualTo(35.08);
            assertThat(quote.getOriginLongitude()).isEqualTo(-106.65);
        });

        mvc.perform(get("/v1/profile/quotes").header("Authorization", "Bearer jaden-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("Uber"));

        mvc.perform(get("/v1/profile/quotes").header("Authorization", "Bearer other-token"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/v1/profile").header("Authorization", "Bearer stale-jaden-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REAUTHENTICATION_REQUIRED"));

        assertThat(profiles.count()).isEqualTo(1);
        assertThat(quotes.count()).isEqualTo(1);

        mvc.perform(delete("/v1/profile").header("Authorization", "Bearer jaden-token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(profiles.count()).isZero();
        assertThat(quotes.count()).isZero();

        mvc.perform(delete("/v1/profile").header("Authorization", "Bearer jaden-token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private String profileBody(String preferredName, String city) {
        return """
                {"preferredName":"%s","homeCity":"%s","homeRegion":"NM","homeCountry":"US",
                 "termsVersion":"2026-09-08","privacyVersion":"2026-09-08",
                 "acceptTerms":true,"acceptPrivacy":true}
                """.formatted(preferredName, city);
    }

    private String quoteBody() {
        return """
                {"provider":"Uber","rideType":"UberX","quotedPrice":16.95,"currency":"USD",
                 "originLatitude":35.08049,"originLongitude":-106.65049,"destinationLatitude":35.12049,
                 "destinationLongitude":-106.57049,"marketCity":"Albuquerque","marketRegion":"NM",
                 "marketCountry":"US","originZone":"Downtown","destinationZone":"Uptown",
                 "roadDistanceMiles":6.2,"travelMinutes":18,"observedAt":"%s",
                 "source":"user-observed-web","website":""}
                """.formatted(Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
    }

    @TestConfiguration
    static class TestAuthConfiguration {
        @Bean
        @Primary
        FirebaseTokenVerifier testTokenVerifier() {
            return token -> switch (token) {
                case "jaden-token" -> new VerifiedIdentity("firebase-jaden", Instant.now());
                case "stale-jaden-token" -> new VerifiedIdentity(
                        "firebase-jaden", Instant.now().minus(10, ChronoUnit.MINUTES));
                case "other-token" -> new VerifiedIdentity("firebase-other", Instant.now());
                case "unsafe-message-token" -> throw new InvalidAuthenticationException(
                        "INVALID_AUTH_TOKEN", "Signed out \"now\".\nRetry.");
                default -> throw new InvalidAuthenticationException(
                        "INVALID_AUTH_TOKEN", "Your sign-in expired or could not be verified.");
            };
        }
    }
}

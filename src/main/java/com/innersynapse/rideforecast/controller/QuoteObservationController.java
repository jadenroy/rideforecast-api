package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.dto.MarketBenchmarkResponse;
import com.innersynapse.rideforecast.dto.QuoteAssessmentResponse;
import com.innersynapse.rideforecast.dto.QuoteObservationRequest;
import com.innersynapse.rideforecast.dto.QuoteObservationResponse;
import com.innersynapse.rideforecast.dto.QuoteSaveResult;
import com.innersynapse.rideforecast.service.QuoteObservationService;
import com.innersynapse.rideforecast.service.UserProfileService;
import com.innersynapse.rideforecast.auth.AuthenticatedRequest;
import com.innersynapse.rideforecast.auth.InvalidAuthenticationException;
import com.innersynapse.rideforecast.auth.VerifiedIdentity;
import com.innersynapse.rideforecast.model.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/quotes")
@Validated
public class QuoteObservationController {

    private final QuoteObservationService service;
    private final UserProfileService profiles;
    private final boolean allowAnonymousQuotes;

    public QuoteObservationController(QuoteObservationService service, UserProfileService profiles,
                                      @Value("${rideforecast.auth.allow-anonymous-quotes:true}") boolean allowAnonymousQuotes) {
        this.service = service;
        this.profiles = profiles;
        this.allowAnonymousQuotes = allowAnonymousQuotes;
    }

    @PostMapping
    public ResponseEntity<QuoteObservationResponse> create(
            @Valid @RequestBody QuoteObservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        VerifiedIdentity identity = AuthenticatedRequest.optional(httpRequest);
        UserProfile profile = identity == null ? null : profiles.find(identity.uid()).orElseThrow(() ->
                new InvalidAuthenticationException("PROFILE_REQUIRED", "Complete your RideForecast profile before saving a quote."));
        if (profile == null && !allowAnonymousQuotes) {
            throw new InvalidAuthenticationException("AUTH_REQUIRED", "Sign in to save a quote to RideForecast.");
        }
        QuoteSaveResult result = service.save(request, idempotencyKey, profile == null ? null : profile.getId());
        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(result.observation());
    }

    @GetMapping("/recent")
    public List<QuoteObservationResponse> recent(
            @RequestParam String marketCity,
            @RequestParam String marketRegion,
            @RequestParam String marketCountry
    ) {
        return service.recent(marketCity, marketRegion, marketCountry);
    }

    @GetMapping("/benchmark")
    public MarketBenchmarkResponse benchmark(
            @RequestParam String marketCity,
            @RequestParam String marketRegion,
            @RequestParam String marketCountry,
            @RequestParam String provider,
            @RequestParam String rideType,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
            @RequestParam(required = false) @Positive Double distanceMiles,
            @RequestParam(required = false) @Positive Double travelMinutes,
            @RequestParam(required = false) String originZone,
            @RequestParam(required = false) String destinationZone
    ) {
        return service.benchmark(
                marketCity,
                marketRegion,
                marketCountry,
                provider,
                rideType,
                days,
                distanceMiles,
                travelMinutes,
                originZone,
                destinationZone
        );
    }

    @GetMapping("/assess")
    public QuoteAssessmentResponse assess(
            @RequestParam String marketCity,
            @RequestParam String marketRegion,
            @RequestParam String marketCountry,
            @RequestParam String provider,
            @RequestParam String rideType,
            @RequestParam @Positive double quotedPrice,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
            @RequestParam(required = false) @Positive Double distanceMiles,
            @RequestParam(required = false) @Positive Double travelMinutes,
            @RequestParam(required = false) String originZone,
            @RequestParam(required = false) String destinationZone
    ) {
        return service.assess(
                marketCity,
                marketRegion,
                marketCountry,
                provider,
                rideType,
                quotedPrice,
                days,
                distanceMiles,
                travelMinutes,
                originZone,
                destinationZone
        );
    }
}

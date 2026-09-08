package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.auth.AuthenticatedRequest;
import com.innersynapse.rideforecast.auth.VerifiedIdentity;
import com.innersynapse.rideforecast.dto.QuoteObservationResponse;
import com.innersynapse.rideforecast.dto.UserProfileRequest;
import com.innersynapse.rideforecast.dto.UserProfileResponse;
import com.innersynapse.rideforecast.model.UserProfile;
import com.innersynapse.rideforecast.service.QuoteObservationService;
import com.innersynapse.rideforecast.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/profile")
public class UserProfileController {
    private final UserProfileService profiles;
    private final QuoteObservationService quotes;

    public UserProfileController(UserProfileService profiles, QuoteObservationService quotes) {
        this.profiles = profiles;
        this.quotes = quotes;
    }

    @GetMapping
    public UserProfileResponse get(HttpServletRequest request) {
        return profiles.get(AuthenticatedRequest.require(request).uid());
    }

    @PutMapping
    public UserProfileResponse save(@Valid @RequestBody UserProfileRequest body, HttpServletRequest request) {
        return profiles.save(AuthenticatedRequest.require(request).uid(), body);
    }

    @GetMapping("/quotes")
    public List<QuoteObservationResponse> quotes(HttpServletRequest request) {
        VerifiedIdentity identity = AuthenticatedRequest.require(request);
        UserProfile profile = profiles.require(identity.uid());
        return quotes.recentByOwner(profile.getId());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(HttpServletRequest request) {
        profiles.delete(AuthenticatedRequest.require(request).uid());
        return ResponseEntity.noContent().build();
    }
}

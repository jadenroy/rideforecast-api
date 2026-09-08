package com.innersynapse.rideforecast.service;

import com.innersynapse.rideforecast.dto.UserProfileRequest;
import com.innersynapse.rideforecast.dto.UserProfileResponse;
import com.innersynapse.rideforecast.exception.ProfileNotFoundException;
import com.innersynapse.rideforecast.model.UserProfile;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import com.innersynapse.rideforecast.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {
    private final UserProfileRepository profiles;
    private final QuoteObservationRepository quotes;

    public UserProfileService(UserProfileRepository profiles, QuoteObservationRepository quotes) {
        this.profiles = profiles;
        this.quotes = quotes;
    }

    public Optional<UserProfile> find(String firebaseUid) {
        return profiles.findByFirebaseUid(firebaseUid);
    }

    public UserProfile require(String firebaseUid) {
        return find(firebaseUid).orElseThrow(() -> new ProfileNotFoundException("Complete your RideForecast profile first."));
    }

    public UserProfileResponse get(String firebaseUid) {
        return response(require(firebaseUid));
    }

    @Transactional
    public UserProfileResponse save(String firebaseUid, UserProfileRequest request) {
        Instant now = Instant.now();
        UserProfile profile = find(firebaseUid).orElseGet(() -> new UserProfile(
                UUID.randomUUID(), firebaseUid, clean(request.preferredName()), nullable(request.homeCity()),
                nullable(request.homeRegion()), country(request.homeCountry()), clean(request.termsVersion()),
                now, clean(request.privacyVersion()), now, now, now
        ));
        if (profiles.existsById(profile.getId())) {
            profile.update(clean(request.preferredName()), nullable(request.homeCity()), nullable(request.homeRegion()),
                    country(request.homeCountry()), clean(request.termsVersion()), clean(request.privacyVersion()), now);
        }
        return response(profiles.save(profile));
    }

    @Transactional
    public void delete(String firebaseUid) {
        UserProfile profile = require(firebaseUid);
        quotes.deleteByOwnerId(profile.getId());
        profiles.delete(profile);
    }

    private UserProfileResponse response(UserProfile profile) {
        return new UserProfileResponse(profile.getId(), profile.getPreferredName(), profile.getHomeCity(),
                profile.getHomeRegion(), profile.getHomeCountry(), profile.getTermsVersion(),
                profile.getTermsAcceptedAt(), profile.getPrivacyVersion(), profile.getPrivacyAcceptedAt(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String nullable(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String country(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned.toUpperCase(Locale.ROOT);
    }
}

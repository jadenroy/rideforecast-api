package com.innersynapse.rideforecast.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank @Size(max = 80) String preferredName,
        @Size(max = 100) String homeCity,
        @Size(max = 100) String homeRegion,
        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "must be a two-letter country code") String homeCountry,
        @NotBlank @Size(max = 30) String termsVersion,
        @NotBlank @Size(max = 30) String privacyVersion,
        @AssertTrue(message = "must be accepted") boolean acceptTerms,
        @AssertTrue(message = "must be accepted") boolean acceptPrivacy
) {
}

package com.innersynapse.rideforecast.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;

public final class AuthenticatedRequest {
    private AuthenticatedRequest() {
    }

    public static VerifiedIdentity optional(HttpServletRequest request) {
        return (VerifiedIdentity) request.getAttribute(FirebaseAuthenticationFilter.IDENTITY_ATTRIBUTE);
    }

    public static VerifiedIdentity require(HttpServletRequest request) {
        VerifiedIdentity identity = optional(request);
        if (identity == null) {
            throw new InvalidAuthenticationException("AUTH_REQUIRED", "Sign in to use this account feature.");
        }
        return identity;
    }

    public static VerifiedIdentity requireRecent(HttpServletRequest request, Duration maximumAge) {
        VerifiedIdentity identity = require(request);
        Instant cutoff = Instant.now().minus(maximumAge);
        if (identity.authenticatedAt() == null || identity.authenticatedAt().isBefore(cutoff)) {
            throw new InvalidAuthenticationException(
                    "REAUTHENTICATION_REQUIRED",
                    "For your security, sign out and sign in again before deleting your account."
            );
        }
        return identity;
    }
}

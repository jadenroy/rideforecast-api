package com.innersynapse.rideforecast.auth;

import jakarta.servlet.http.HttpServletRequest;

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
}

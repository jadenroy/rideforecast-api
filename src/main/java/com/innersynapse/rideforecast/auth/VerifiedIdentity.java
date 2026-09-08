package com.innersynapse.rideforecast.auth;

import java.time.Instant;

public record VerifiedIdentity(String uid, Instant authenticatedAt) {
}

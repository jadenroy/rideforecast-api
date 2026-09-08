package com.innersynapse.rideforecast.auth;

public interface FirebaseTokenVerifier {
    VerifiedIdentity verify(String idToken);
}

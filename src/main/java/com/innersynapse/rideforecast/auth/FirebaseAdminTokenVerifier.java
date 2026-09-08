package com.innersynapse.rideforecast.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private final String serviceAccountJson;
    private volatile FirebaseAuth firebaseAuth;

    public FirebaseAdminTokenVerifier(
            @Value("${rideforecast.auth.firebase-service-account-json:}") String serviceAccountJson
    ) {
        this.serviceAccountJson = serviceAccountJson;
    }

    @Override
    public VerifiedIdentity verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth().verifyIdToken(idToken);
            return new VerifiedIdentity(token.getUid());
        } catch (FirebaseAuthException exception) {
            throw new InvalidAuthenticationException("INVALID_AUTH_TOKEN", "Your sign-in expired or could not be verified.");
        }
    }

    private FirebaseAuth firebaseAuth() {
        FirebaseAuth current = firebaseAuth;
        if (current != null) return current;

        synchronized (this) {
            if (firebaseAuth != null) return firebaseAuth;
            if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
                throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_JSON is required when authentication is enabled.");
            }
            try {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
                );
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp app = FirebaseApp.getApps().stream()
                        .filter(item -> item.getName().equals("rideforecast-auth"))
                        .findFirst()
                        .orElseGet(() -> FirebaseApp.initializeApp(options, "rideforecast-auth"));
                firebaseAuth = FirebaseAuth.getInstance(app);
                return firebaseAuth;
            } catch (IOException exception) {
                throw new IllegalStateException("Firebase service-account JSON is invalid.", exception);
            }
        }
    }
}

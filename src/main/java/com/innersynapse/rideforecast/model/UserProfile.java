package com.innersynapse.rideforecast.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profile_firebase_uid", columnList = "firebase_uid", unique = true)
})
public class UserProfile {
    @Id
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    private String firebaseUid;

    @Column(name = "preferred_name", nullable = false, length = 80)
    private String preferredName;

    @Column(name = "home_city", length = 100)
    private String homeCity;

    @Column(name = "home_region", length = 100)
    private String homeRegion;

    @Column(name = "home_country", length = 2)
    private String homeCountry;

    @Column(name = "terms_version", nullable = false, length = 30)
    private String termsVersion;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Column(name = "privacy_version", nullable = false, length = 30)
    private String privacyVersion;

    @Column(name = "privacy_accepted_at", nullable = false)
    private Instant privacyAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    public UserProfile(UUID id, String firebaseUid, String preferredName, String homeCity,
                       String homeRegion, String homeCountry, String termsVersion,
                       Instant termsAcceptedAt, String privacyVersion, Instant privacyAcceptedAt,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.preferredName = preferredName;
        this.homeCity = homeCity;
        this.homeRegion = homeRegion;
        this.homeCountry = homeCountry;
        this.termsVersion = termsVersion;
        this.termsAcceptedAt = termsAcceptedAt;
        this.privacyVersion = privacyVersion;
        this.privacyAcceptedAt = privacyAcceptedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String preferredName, String homeCity, String homeRegion, String homeCountry,
                       String termsVersion, String privacyVersion, Instant now) {
        this.preferredName = preferredName;
        this.homeCity = homeCity;
        this.homeRegion = homeRegion;
        this.homeCountry = homeCountry;
        if (!this.termsVersion.equals(termsVersion)) {
            this.termsVersion = termsVersion;
            this.termsAcceptedAt = now;
        }
        if (!this.privacyVersion.equals(privacyVersion)) {
            this.privacyVersion = privacyVersion;
            this.privacyAcceptedAt = now;
        }
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getPreferredName() { return preferredName; }
    public String getHomeCity() { return homeCity; }
    public String getHomeRegion() { return homeRegion; }
    public String getHomeCountry() { return homeCountry; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public String getPrivacyVersion() { return privacyVersion; }
    public Instant getPrivacyAcceptedAt() { return privacyAcceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

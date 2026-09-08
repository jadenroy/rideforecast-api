package com.innersynapse.rideforecast.repository;

import com.innersynapse.rideforecast.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByFirebaseUid(String firebaseUid);
}

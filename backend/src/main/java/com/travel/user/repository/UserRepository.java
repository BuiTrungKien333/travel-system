package com.travel.user.repository;

import com.travel.user.entity.User;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> lockByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.googleSubject = :subject")
    Optional<User> lockByGoogleSubject(String subject);

    // Avoid loading password hashes for every authenticated request.
    Optional<AuthState> findAuthStateById(UUID id);

    interface AuthState {
        String getEmail();
        Instant getEmailVerifiedAt();
        Instant getDeletedAt();
        Instant getLockedAt();
        com.travel.user.enums.Role getRole();
        long getSessionVersion();
    }
}

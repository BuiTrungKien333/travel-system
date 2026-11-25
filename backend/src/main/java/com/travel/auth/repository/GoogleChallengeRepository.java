package com.travel.auth.repository;

import com.travel.auth.entity.GoogleChallenge;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface GoogleChallengeRepository extends JpaRepository<GoogleChallenge, UUID> {
    @Modifying
    @Query("delete from GoogleChallenge c where c.id = :id and c.expiresAt > :now")
    int consume(UUID id, Instant now);

    @Modifying
    @Query("delete from GoogleChallenge c where c.expiresAt <= :now")
    int deleteExpired(Instant now);
}

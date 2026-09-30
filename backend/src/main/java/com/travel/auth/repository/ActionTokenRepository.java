package com.travel.auth.repository;

import com.travel.auth.entity.ActionPurpose;
import com.travel.auth.entity.ActionToken;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActionTokenRepository extends JpaRepository<ActionToken, UUID> {

    Optional<ActionToken> findByTokenHashAndPurpose(String tokenHash, ActionPurpose purpose);

    Optional<ActionToken> findByUserIdAndPurpose(UUID userId, ActionPurpose purpose);

    @Modifying
    @Query("delete from ActionToken t where t.tokenHash = :hash and t.purpose = :purpose and t.expiresAt > :now")
    int consume(String hash, ActionPurpose purpose, Instant now);

    @Modifying
    @Query("delete from ActionToken t where t.userId = :userId and t.purpose = :purpose")
    int revoke(UUID userId, ActionPurpose purpose);

    @Modifying
    @Query("delete from ActionToken t where t.userId = :userId")
    int revokeAll(UUID userId);
}

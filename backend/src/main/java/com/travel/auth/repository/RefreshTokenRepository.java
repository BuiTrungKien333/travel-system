package com.travel.auth.repository;

import com.travel.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    // DELETE có điều kiện đảm bảo chỉ một request thắng khi refresh đồng thời.
    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash and t.userId = :userId and t.expiresAt > :now")
    int consume(String tokenHash, UUID userId, Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash")
    int revoke(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId")
    int revokeAllForUser(UUID userId);
}

package com.travel.auth.repository;

import com.travel.auth.entity.GoogleLinkTicket;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.*;

public interface GoogleLinkTicketRepository extends JpaRepository<GoogleLinkTicket, UUID> {

    Optional<GoogleLinkTicket> findByTokenHash(String hash);

    Optional<GoogleLinkTicket> findByUserIdAndGoogleSubject(UUID userId, String subject);

    @Modifying
    @Query("delete from GoogleLinkTicket t where t.tokenHash = :hash and t.expiresAt > :now")
    int consume(String hash, Instant now);

    @Modifying
    @Query("delete from GoogleLinkTicket t where t.googleSubject = :subject")
    int revokeForSubject(String subject);

    @Modifying
    @Query("delete from GoogleLinkTicket t where t.expiresAt <= :now")
    int deleteExpired(Instant now);
}

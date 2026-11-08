package com.travel.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_google_link_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleLinkTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "google_subject", nullable = false)
    private String googleSubject;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public GoogleLinkTicket(UUID userId, String subject, String email, String hash, long version, Instant created, Instant expiry) {
        this.userId = userId;
        this.googleSubject = subject;
        this.email = email;
        this.tokenHash = hash;
        this.sessionVersion = version;
        this.createdAt = created;
        this.expiresAt = expiry;
    }
}

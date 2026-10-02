package com.travel.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_google_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleChallenge {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "nonce_hash", nullable = false, length = 64) private String nonceHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    public GoogleChallenge(String nonceHash, Instant expiresAt) { this.nonceHash = nonceHash; this.expiresAt = expiresAt; }
}

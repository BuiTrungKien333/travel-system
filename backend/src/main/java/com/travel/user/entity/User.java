package com.travel.user.entity;

import com.travel.common.persistence.BaseEntity;
import com.travel.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    public boolean isEmailVerified() { return emailVerifiedAt != null; }
}

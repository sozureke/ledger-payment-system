package com.sozureke.ledger.auth;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private AuthToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static AuthToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        return new AuthToken(userId, tokenHash, expiresAt);
    }

    public boolean isActiveAt(Instant moment) {
        return revokedAt == null && expiresAt.isAfter(moment);
    }

    public void revoke(Instant moment) {
        if (revokedAt == null) {
            this.revokedAt = moment;
        }
    }
}

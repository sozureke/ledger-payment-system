package com.sozureke.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sozureke.ledger.support.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuthTokenRepositoryTest {

    @Autowired
    AuthTokenRepository tokenRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void findsTokenByHashAndKeepsRevocationState() {
        UUID userId = insertCustomer();
        Instant expiresAt = Instant.now().plusSeconds(1800);
        tokenRepository.saveAndFlush(AuthToken.issue(userId, "hash-1", expiresAt));
        entityManager.clear();

        AuthToken found = tokenRepository.findByTokenHash("hash-1").orElseThrow();

        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getRevokedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.isActiveAt(Instant.now())).isTrue();
    }

    @Test
    void doesNotFindUnknownHash() {
        assertThat(tokenRepository.findByTokenHash("missing")).isEmpty();
    }

    @Test
    void persistsRevocation() {
        UUID userId = insertCustomer();
        AuthToken token = tokenRepository.saveAndFlush(
                AuthToken.issue(userId, "hash-2", Instant.now().plusSeconds(1800)));
        token.revoke(Instant.now());
        tokenRepository.saveAndFlush(token);
        entityManager.clear();

        AuthToken found = tokenRepository.findByTokenHash("hash-2").orElseThrow();

        assertThat(found.getRevokedAt()).isNotNull();
        assertThat(found.isActiveAt(Instant.now())).isFalse();
    }

    @Test
    void rejectsDuplicateTokenHash() {
        UUID userId = insertCustomer();
        Instant expiresAt = Instant.now().plusSeconds(1800);
        tokenRepository.saveAndFlush(AuthToken.issue(userId, "same", expiresAt));

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(AuthToken.issue(userId, "same", expiresAt)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("auth_tokens_hash_uk");
    }

    @Test
    void rejectsTokenThatExpiresBeforeItIsCreated() {
        UUID userId = insertCustomer();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO auth_tokens (user_id, token_hash, expires_at) VALUES (?, 'hash-3', now() - interval '1 hour')",
                userId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("auth_tokens_expiry_chk");
    }

    @Test
    void rejectsTokenForMissingUser() {
        assertThatThrownBy(() -> tokenRepository.saveAndFlush(
                AuthToken.issue(UUID.randomUUID(), "hash-4", Instant.now().plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("auth_tokens_user_id_fkey");
    }

    private UUID insertCustomer() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (kind, name, email, password_hash) VALUES ('CUSTOMER', 'Ann', ?, 'hash') RETURNING id",
                UUID.class, UUID.randomUUID() + "@example.com");
    }
}

package com.sozureke.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sozureke.ledger.user.User;
import com.sozureke.ledger.user.UserService;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    private AuthTokenRepository tokenRepository;
    private UserService userService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(AuthTokenRepository.class);
        userService = mock(UserService.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        authService = new AuthService(tokenRepository, userService, clock, TTL);
    }

    @Test
    void loginStoresOnlyTheHashOfTheIssuedToken() {
        UUID userId = UUID.randomUUID();
        User user = User.customer("ann@example.com", "Ann", "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userService.authenticate("ann@example.com", "secret")).thenReturn(Optional.of(user));

        IssuedToken issued = authService.login("ann@example.com", "secret");

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(tokenRepository).save(captor.capture());
        AuthToken stored = captor.getValue();
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.getTokenHash()).isEqualTo(TokenCodec.hash(issued.accessToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(issued.accessToken());
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void loginThrowsInvalidCredentialsAndStoresNothingWhenAuthenticationFails() {
        when(userService.authenticate("ann@example.com", "wrong")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ann@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenRepository, never()).save(any(AuthToken.class));
    }

    @Test
    void resolveUserIdReturnsOwnerOfActiveToken() {
        UUID userId = UUID.randomUUID();
        AuthToken token = AuthToken.issue(userId, TokenCodec.hash("raw"), NOW.plusSeconds(60));
        when(tokenRepository.findByTokenHash(TokenCodec.hash("raw"))).thenReturn(Optional.of(token));

        assertThat(authService.resolveUserId("raw")).contains(userId);
    }

    @Test
    void resolveUserIdIsEmptyForExpiredToken() {
        AuthToken token = AuthToken.issue(UUID.randomUUID(), TokenCodec.hash("raw"), NOW.minusSeconds(1));
        when(tokenRepository.findByTokenHash(TokenCodec.hash("raw"))).thenReturn(Optional.of(token));

        assertThat(authService.resolveUserId("raw")).isEmpty();
    }

    @Test
    void resolveUserIdIsEmptyForRevokedToken() {
        AuthToken token = AuthToken.issue(UUID.randomUUID(), TokenCodec.hash("raw"), NOW.plusSeconds(60));
        token.revoke(NOW.minusSeconds(5));
        when(tokenRepository.findByTokenHash(TokenCodec.hash("raw"))).thenReturn(Optional.of(token));

        assertThat(authService.resolveUserId("raw")).isEmpty();
    }

    @Test
    void resolveUserIdIsEmptyForUnknownToken() {
        when(tokenRepository.findByTokenHash(TokenCodec.hash("nope"))).thenReturn(Optional.empty());

        assertThat(authService.resolveUserId("nope")).isEmpty();
    }

    @Test
    void logoutRevokesTheToken() {
        AuthToken token = AuthToken.issue(UUID.randomUUID(), TokenCodec.hash("raw"), NOW.plusSeconds(60));
        when(tokenRepository.findByTokenHash(TokenCodec.hash("raw"))).thenReturn(Optional.of(token));

        authService.logout("raw");

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
    }
}

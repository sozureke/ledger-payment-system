package com.sozureke.ledger.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sozureke.ledger.user.User;
import com.sozureke.ledger.user.UserService;

@Service
public class AuthService {

    private final AuthTokenRepository tokenRepository;
    private final UserService userService;
    private final Clock clock;
    private final Duration tokenTtl;

    public AuthService(AuthTokenRepository tokenRepository, UserService userService, Clock clock,
            @Value("${ledger.auth.token-ttl:PT30M}") Duration tokenTtl) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public IssuedToken login(String email, String rawPassword) {
        User user = userService.authenticate(email, rawPassword)
                .orElseThrow(InvalidCredentialsException::new);
        String rawToken = TokenCodec.generate();
        Instant expiresAt = clock.instant().plus(tokenTtl);
        tokenRepository.save(AuthToken.issue(user.getId(), TokenCodec.hash(rawToken), expiresAt));
        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional
    public void logout(String rawToken) {
        tokenRepository.findByTokenHash(TokenCodec.hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> resolveUserId(String rawToken) {
        return tokenRepository.findByTokenHash(TokenCodec.hash(rawToken))
                .filter(token -> token.isActiveAt(clock.instant()))
                .map(AuthToken::getUserId);
    }
}

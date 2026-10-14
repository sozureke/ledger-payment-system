package com.sozureke.ledger.auth;

import java.time.Instant;

public record IssuedToken(String accessToken, Instant expiresAt) {

    @Override
    public String toString() {
        return "IssuedToken{accessToken='***', expiresAt=" + expiresAt + "}";
    }
}

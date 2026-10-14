package com.sozureke.ledger.auth.dto;

import java.time.Instant;

import com.sozureke.ledger.auth.IssuedToken;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), "Bearer", token.expiresAt());
    }

    @Override
    public String toString() {
        return "TokenResponse{accessToken='***', tokenType='" + tokenType + "', expiresAt=" + expiresAt + "}";
    }
}

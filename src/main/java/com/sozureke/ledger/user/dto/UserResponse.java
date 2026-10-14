package com.sozureke.ledger.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.sozureke.ledger.user.User;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}

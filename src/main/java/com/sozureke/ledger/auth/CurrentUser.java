package com.sozureke.ledger.auth;

import java.util.Objects;
import java.util.UUID;

public record CurrentUser(UUID id) {

    public CurrentUser {
        Objects.requireNonNull(id, "id");
    }
}

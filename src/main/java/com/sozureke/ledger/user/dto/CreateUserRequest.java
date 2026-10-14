package com.sozureke.ledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "CreateUserRequest{name='" + name + "'}";
    }
}

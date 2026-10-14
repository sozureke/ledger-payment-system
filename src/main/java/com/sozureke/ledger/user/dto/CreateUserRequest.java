package com.sozureke.ledger.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 72) String password) {

    @Override
    public String toString() {
        return "CreateUserRequest{name='" + name + "', email='***'}";
    }
}

package com.sozureke.ledger.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sozureke.ledger.auth.CurrentUser;
import com.sozureke.ledger.user.dto.CreateUserRequest;
import com.sozureke.ledger.user.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createCustomer(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createCustomer(request.email(), request.name(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));

    }

    @GetMapping("/me")
    public UserResponse getCurrent(CurrentUser current) {
        return UserResponse.from(userService.getById(current.id()));
    }
}

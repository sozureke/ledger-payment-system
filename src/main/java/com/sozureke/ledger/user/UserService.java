package com.sozureke.ledger.user;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createCustomer(String name, String rawPassword) {
        String passwordHash = passwordEncoder.encode(rawPassword);
        return userRepository.save(User.customer(name, passwordHash));
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findByIdAndKind(id, UserKind.CUSTOMER)
                .orElseThrow(UserNotFoundException::new);
    }
}

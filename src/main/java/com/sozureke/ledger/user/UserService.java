package com.sozureke.ledger.user;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public User createCustomer(String email, String name, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        try {
            return userRepository.saveAndFlush(User.customer(normalizedEmail, name, passwordHash));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findByIdAndKind(id, UserKind.CUSTOMER)
                .orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String email, String rawPassword) {
        Optional<User> user = userRepository.findByEmailAndKind(normalizeEmail(email), UserKind.CUSTOMER);
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        return user.filter(found -> matches);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

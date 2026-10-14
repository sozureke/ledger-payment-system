package com.sozureke.ledger.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByIdAndKind(UUID id, UserKind kind);

    boolean existsByEmail(String email);
}

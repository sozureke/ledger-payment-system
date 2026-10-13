package com.sozureke.ledger.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sozureke.ledger.support.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    @Test
    void savesCustomerAndReadsItBack() {
        User user = User.customer("Ann", "hash");

        userRepository.saveAndFlush(user);
        entityManager.clear();
        User found = userRepository.findById(user.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getKind()).isEqualTo(UserKind.CUSTOMER);
        assertThat(found.getName()).isEqualTo("Ann");
        assertThat(found.getPasswordHash()).isEqualTo("hash");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsCustomerWithoutPasswordHash() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (kind, name) VALUES ('CUSTOMER', 'Ann')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_password_chk");
    }

    @Test
    void rejectsSystemWithPasswordHash() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (kind, name, password_hash) VALUES ('SYSTEM', 'Ann', 'hash')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_password_chk");
    }

    @Test
    void rejectsUnknownKind() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (kind, name, password_hash) VALUES ('ADMIN', 'Ann', 'hash')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_kind_chk");
    }

    @Test
    void findsCustomerByIdAndKind() {
        User customer = User.customer("Ann", "hash");
        userRepository.saveAndFlush(customer);
        entityManager.clear();

        Optional<User> result = userRepository.findByIdAndKind(customer.getId(), UserKind.CUSTOMER);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(customer.getId());
    }

    @Test
    void doesNotFindSystemUserAsCustomer() {
        User customer = User.customer("Ann", "hash");
        User system = User.system("bank");
        userRepository.saveAndFlush(customer);
        userRepository.saveAndFlush(system);
        entityManager.clear();

        Optional<User> result = userRepository.findByIdAndKind(system.getId(), UserKind.CUSTOMER);
        assertThat(result).isEmpty();

    }
}

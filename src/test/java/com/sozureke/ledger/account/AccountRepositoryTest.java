package com.sozureke.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
class AccountRepositoryTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void savesAccountAndReadsItBack() {
        UUID userId = insertCustomer("Ann");
        Account account = Account.open(userId, AccountCurrency.EUR);

        accountRepository.saveAndFlush(account);
        entityManager.clear();
        Account found = accountRepository.findById(account.getId()).orElseThrow();

        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getCurrency()).isEqualTo(AccountCurrency.EUR);
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(found.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByIdAndUserIdFindsOwnAccount() {
        UUID userId = insertCustomer("Ann");
        Account account = accountRepository.saveAndFlush(Account.open(userId, AccountCurrency.EUR));
        entityManager.clear();

        Optional<Account> result = accountRepository.findByIdAndUserId(account.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(account.getId());
    }

    @Test
    void findByIdAndUserIdDoesNotFindForeignAccount() {
        UUID owner = insertCustomer("Ann");
        UUID stranger = insertCustomer("Bob");
        Account account = accountRepository.saveAndFlush(Account.open(owner, AccountCurrency.EUR));
        entityManager.clear();

        Optional<Account> result = accountRepository.findByIdAndUserId(account.getId(), stranger);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdReturnsOnlyOwnAccounts() {
        UUID ann = insertCustomer("Ann");
        UUID bob = insertCustomer("Bob");
        accountRepository.saveAndFlush(Account.open(ann, AccountCurrency.EUR));
        accountRepository.saveAndFlush(Account.open(ann, AccountCurrency.USD));
        accountRepository.saveAndFlush(Account.open(bob, AccountCurrency.EUR));
        entityManager.clear();

        List<Account> result = accountRepository.findByUserId(ann);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(account -> account.getUserId().equals(ann));
    }

    @Test
    void existsByUserIdAndCurrencyIsTrueOnlyForMatchingCurrency() {
        UUID userId = insertCustomer("Ann");
        accountRepository.saveAndFlush(Account.open(userId, AccountCurrency.EUR));

        assertThat(accountRepository.existsByUserIdAndCurrency(userId, AccountCurrency.EUR)).isTrue();
        assertThat(accountRepository.existsByUserIdAndCurrency(userId, AccountCurrency.USD)).isFalse();
    }

    @Test
    void rejectsSecondAccountInSameCurrencyForSameUser() {
        UUID userId = insertCustomer("Ann");
        accountRepository.saveAndFlush(Account.open(userId, AccountCurrency.EUR));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(Account.open(userId, AccountCurrency.EUR)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_user_currency_uk");
    }

    @Test
    void rejectsCurrencyThatIsNotThreeUpperCaseLetters() {
        UUID userId = insertCustomer("Ann");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO accounts (user_id, currency) VALUES (?, 'eur')", userId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_currency_chk");
    }

    @Test
    void rejectsAccountForMissingUser() {
        Account orphan = Account.open(UUID.randomUUID(), AccountCurrency.EUR);

        assertThatThrownBy(() -> accountRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_user_id_fkey");
    }

    private UUID insertCustomer(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (kind, name, password_hash) VALUES ('CUSTOMER', ?, 'hash') RETURNING id",
                UUID.class, name);
    }
}

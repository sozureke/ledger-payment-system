package com.sozureke.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    @Test
    void openSavesNewActiveAccountWithZeroBalance() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.existsByUserIdAndCurrency(userId, AccountCurrency.EUR)).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.open(userId, AccountCurrency.EUR);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCurrency()).isEqualTo(AccountCurrency.EUR);
        assertThat(result.getBalance()).isEqualByComparingTo("0");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void openThrowsAlreadyExistsAndDoesNotSaveWhenAccountInCurrencyExists() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.existsByUserIdAndCurrency(userId, AccountCurrency.EUR)).thenReturn(true);

        assertThatThrownBy(() -> accountService.open(userId, AccountCurrency.EUR))
                .isInstanceOf(AccountAlreadyExistsException.class);

        verify(accountRepository, never()).saveAndFlush(any(Account.class));
    }

    @Test
    void openTranslatesConstraintViolationFromRaceIntoAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.existsByUserIdAndCurrency(userId, AccountCurrency.EUR)).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> accountService.open(userId, AccountCurrency.EUR))
                .isInstanceOf(AccountAlreadyExistsException.class);
    }

    @Test
    void getOwnedReturnsAccountOfTheOwner() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = Account.open(userId, AccountCurrency.USD);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));

        Account result = accountService.getOwned(userId, accountId);

        assertThat(result).isSameAs(account);
    }

    @Test
    void getOwnedThrowsDomainAccountNotFoundWhenMissingOrForeign() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getOwned(userId, accountId))
                .isInstanceOf(com.sozureke.ledger.account.AccountNotFoundException.class)
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void listByUserReturnsRepositoryResult() {
        UUID userId = UUID.randomUUID();
        List<Account> accounts = List.of(
                Account.open(userId, AccountCurrency.EUR),
                Account.open(userId, AccountCurrency.USD));
        when(accountRepository.findByUserId(userId)).thenReturn(accounts);

        List<Account> result = accountService.listByUser(userId);

        assertThat(result).isEqualTo(accounts);
    }
}

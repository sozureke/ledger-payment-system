package com.sozureke.ledger.account;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public Account open(UUID userId, AccountCurrency currency) {
        if (accountRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new AccountAlreadyExistsException();
        }
        try {
            return accountRepository.saveAndFlush(Account.open(userId, currency));
        } catch (DataIntegrityViolationException ex) {
            throw new AccountAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public Account getOwned(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Account> listByUser(UUID userId) {
        return accountRepository.findByUserId(userId);
    }

}

package com.sozureke.ledger.account;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException() {
        super(ErrorCode.ACCOUNT_NOT_FOUND);
    }
}

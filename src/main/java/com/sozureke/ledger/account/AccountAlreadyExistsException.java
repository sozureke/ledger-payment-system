package com.sozureke.ledger.account;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

public class AccountAlreadyExistsException extends DomainException {
    public AccountAlreadyExistsException() {
        super(ErrorCode.ACCOUNT_ALREADY_EXISTS);
    }
}

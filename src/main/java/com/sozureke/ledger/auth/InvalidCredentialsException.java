package com.sozureke.ledger.auth;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}

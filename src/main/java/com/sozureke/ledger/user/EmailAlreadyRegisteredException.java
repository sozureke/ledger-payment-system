package com.sozureke.ledger.user;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }
}

package com.sozureke.ledger.common;

import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {
    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getTitle());
        this.errorCode = errorCode;
    }
}

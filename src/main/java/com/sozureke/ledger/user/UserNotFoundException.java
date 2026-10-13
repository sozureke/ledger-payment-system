package com.sozureke.ledger.user;

import com.sozureke.ledger.common.DomainException;
import com.sozureke.ledger.common.ErrorCode;

public class UserNotFoundException extends DomainException {
  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND);
  }
}

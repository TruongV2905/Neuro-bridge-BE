package com.nqt.common_starter.exception;

import com.nqt.common_starter.constant.ErrorCode;

public class GlobalException extends RuntimeException{
    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    public GlobalException(String message) {
        super(message);
    }

    private ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

}

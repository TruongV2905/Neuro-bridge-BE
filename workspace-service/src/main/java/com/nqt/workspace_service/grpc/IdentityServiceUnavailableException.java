package com.nqt.workspace_service.grpc;

public class IdentityServiceUnavailableException extends RuntimeException {
    public IdentityServiceUnavailableException() { super(); }
    public IdentityServiceUnavailableException(String message) { super(message); }
    public IdentityServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    public IdentityServiceUnavailableException(Throwable cause) { super(cause); }
}


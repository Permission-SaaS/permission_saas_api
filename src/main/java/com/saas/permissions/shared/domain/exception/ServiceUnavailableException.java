package com.saas.permissions.shared.domain.exception;

public abstract class ServiceUnavailableException extends RuntimeException {
    protected ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

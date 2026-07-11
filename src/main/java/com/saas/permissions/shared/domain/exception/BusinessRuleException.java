package com.saas.permissions.shared.domain.exception;

public abstract class BusinessRuleException extends DomainException {
    protected BusinessRuleException(String message) {
        super(message);
    }
}

package com.saas.permissions.billing.domain.subscription.exception;

import java.time.OffsetDateTime;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class ActiveSubscriptionExistsException extends BusinessRuleException {
    public ActiveSubscriptionExistsException(OffsetDateTime expiresAt) {
        super("Client already has an active subscription to this plan, valid until " + expiresAt);
    }
}

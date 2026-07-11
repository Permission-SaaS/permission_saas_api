package com.saas.permissions.modules.billing.domain.subscription.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class PaymentDeclinedException extends BusinessRuleException {
    public PaymentDeclinedException(UUID subscriptionId) {
        super("Payment declined for subscription " + subscriptionId);
    }
}

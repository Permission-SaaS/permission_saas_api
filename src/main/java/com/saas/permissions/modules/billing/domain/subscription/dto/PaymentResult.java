package com.saas.permissions.modules.billing.domain.subscription.dto;

public record PaymentResult(boolean approved, String transactionId) {
}

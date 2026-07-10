package com.saas.permissions.billing.domain.subscription.dto;

public record PaymentResult(boolean approved, String transactionId) {
}

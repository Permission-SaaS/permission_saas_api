package com.saas.permissions.billing.domain.subscription.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID subscriptionId, BigDecimal amount) {
}

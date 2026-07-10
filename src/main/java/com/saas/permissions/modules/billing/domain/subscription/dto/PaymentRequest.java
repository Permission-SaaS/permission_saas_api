package com.saas.permissions.modules.billing.domain.subscription.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID subscriptionId, BigDecimal amount) {
}

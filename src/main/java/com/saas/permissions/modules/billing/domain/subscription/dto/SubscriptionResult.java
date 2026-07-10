package com.saas.permissions.modules.billing.domain.subscription.dto;

import com.saas.permissions.modules.billing.domain.subscription.ApiKey;
import com.saas.permissions.modules.billing.domain.subscription.Subscription;

public record SubscriptionResult(Subscription subscription, ApiKey apiKey) {
}

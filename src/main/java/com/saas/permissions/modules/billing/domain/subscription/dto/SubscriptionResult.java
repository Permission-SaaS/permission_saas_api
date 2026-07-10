package com.saas.permissions.billing.domain.subscription.dto;

import com.saas.permissions.billing.domain.subscription.ApiKey;
import com.saas.permissions.billing.domain.subscription.Subscription;

public record SubscriptionResult(Subscription subscription, ApiKey apiKey) {
}

package com.saas.permissions.billing.api.subscription.dto;

import java.util.UUID;

public record SubscriptionResponse(
        UUID subscriptionId,
        String status,
        String startsAt,
        String expiresAt,
        String apiKey) {
}

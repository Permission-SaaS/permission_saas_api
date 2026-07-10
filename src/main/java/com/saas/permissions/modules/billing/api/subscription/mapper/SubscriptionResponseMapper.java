
package com.saas.permissions.modules.billing.api.subscription.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.billing.api.subscription.dto.SubscriptionResponse;
import com.saas.permissions.modules.billing.domain.subscription.Subscription;
import com.saas.permissions.modules.billing.domain.subscription.dto.SubscriptionResult;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class SubscriptionResponseMapper implements Mapper<SubscriptionResult, SubscriptionResponse> {

    @Override
    public SubscriptionResponse map(SubscriptionResult result) {
        Subscription subscription = result.subscription();

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getStatus().name(),
                subscription.getStartsAt() != null ? subscription.getStartsAt().toString() : null,
                subscription.getExpiresAt() != null ? subscription.getExpiresAt().toString() : null,
                result.apiKey().getPlainKey());
    }
}

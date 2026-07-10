package com.saas.permissions.billing.domain.subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(UUID id);

    Optional<Subscription> findByClientIdAndStatus(UUID clientId, SubscriptionStatus status);
}

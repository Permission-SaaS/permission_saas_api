package com.saas.permissions.modules.billing.domain.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {

    ApiKey save(ApiKey apiKey);

    Optional<ApiKey> findById(UUID id);

    Optional<ApiKey> findBySubscriptionId(UUID subscriptionId);

    List<ApiKey> findAllActive();
}

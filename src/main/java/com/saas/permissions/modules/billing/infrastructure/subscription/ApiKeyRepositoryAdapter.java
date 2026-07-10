package com.saas.permissions.billing.infrastructure.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.saas.permissions.billing.domain.subscription.ApiKey;
import com.saas.permissions.billing.domain.subscription.ApiKeyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpa;

    @Override
    public ApiKey save(ApiKey apiKey) {
        ApiKeyJpaEntity saved = jpa.save(toJpa(apiKey));
        return toDomain(saved, apiKey.getPlainKey());
    }

    @Override
    public Optional<ApiKey> findById(UUID id) {
        return jpa.findById(id).map(entity -> toDomain(entity, null));
    }

    @Override
    public Optional<ApiKey> findBySubscriptionId(UUID subscriptionId) {
        return jpa.findBySubscriptionId(subscriptionId).map(entity -> toDomain(entity, null));
    }

    @Override
    public List<ApiKey> findAllActive() {
        return jpa.findByActiveTrue().stream().map(entity -> toDomain(entity, null)).toList();
    }

    private ApiKeyJpaEntity toJpa(ApiKey apiKey) {
        return ApiKeyJpaEntity.builder()
                .id(apiKey.getId())
                .subscriptionId(apiKey.getSubscriptionId())
                .keyHash(apiKey.getKeyHash())
                .active(apiKey.isActive())
                .revokedAt(apiKey.getRevokedAt())
                .build();
    }

    private ApiKey toDomain(ApiKeyJpaEntity entity, String plainKey) {
        return ApiKey.builder()
                .id(entity.getId())
                .subscriptionId(entity.getSubscriptionId())
                .keyHash(entity.getKeyHash())
                .plainKey(plainKey)
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .revokedAt(entity.getRevokedAt())
                .build();
    }
}

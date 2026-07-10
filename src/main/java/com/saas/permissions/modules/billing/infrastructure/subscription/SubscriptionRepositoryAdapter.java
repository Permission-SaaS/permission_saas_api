package com.saas.permissions.modules.billing.infrastructure.subscription;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.saas.permissions.modules.billing.domain.subscription.Subscription;
import com.saas.permissions.modules.billing.domain.subscription.SubscriptionRepository;
import com.saas.permissions.modules.billing.domain.subscription.SubscriptionStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpa;

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity saved = jpa.save(toJpa(subscription));
        return toDomain(saved);
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByClientIdAndStatus(UUID clientId, SubscriptionStatus status) {
        return jpa.findByClientIdAndStatus(clientId, status).map(this::toDomain);
    }

    private SubscriptionJpaEntity toJpa(Subscription subscription) {
        return SubscriptionJpaEntity.builder()
                .id(subscription.getId())
                .clientId(subscription.getClientId())
                .planId(subscription.getPlanId())
                .status(subscription.getStatus())
                .startsAt(subscription.getStartsAt())
                .expiresAt(subscription.getExpiresAt())
                .build();
    }

    private Subscription toDomain(SubscriptionJpaEntity entity) {
        return Subscription.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .planId(entity.getPlanId())
                .status(entity.getStatus())
                .startsAt(entity.getStartsAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

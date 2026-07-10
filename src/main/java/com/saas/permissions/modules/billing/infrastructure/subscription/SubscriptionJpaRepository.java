package com.saas.permissions.billing.infrastructure.subscription;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saas.permissions.billing.domain.subscription.SubscriptionStatus;

@Repository
interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    Optional<SubscriptionJpaEntity> findByClientIdAndStatus(UUID clientId, SubscriptionStatus status);
}

package com.saas.permissions.billing.infrastructure.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ApiKeyJpaRepository extends JpaRepository<ApiKeyJpaEntity, UUID> {

    Optional<ApiKeyJpaEntity> findBySubscriptionId(UUID subscriptionId);

    List<ApiKeyJpaEntity> findByActiveTrue();
}

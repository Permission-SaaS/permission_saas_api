package com.saas.permissions.billing.infrastructure.plan;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

    Optional<PlanJpaEntity> findByName(String name);
}

package com.saas.permissions.modules.billing.infrastructure.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.saas.permissions.modules.billing.domain.plan.Plan;
import com.saas.permissions.modules.billing.domain.plan.PlanRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlanRepositoryAdapter implements PlanRepository {

    private final PlanJpaRepository jpa;

    @Override
    public List<Plan> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Plan> findById(UUID planId) {
        return jpa.findById(planId).map(this::toDomain);
    }

    @Override
    public Optional<Plan> findByName(String planName) {
        return jpa.findByName(planName).map(this::toDomain);
    }

    @Override
    public Plan save(Plan plan) {
        PlanJpaEntity saved = jpa.save(toJpa(plan));
        return toDomain(saved);
    }

    private PlanJpaEntity toJpa(Plan plan) {
        return PlanJpaEntity.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .maxProjects(plan.getMaxProjects())
                .maxUsersPerProject(plan.getMaxUsersPerProject())
                .price(plan.getPrice())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private Plan toDomain(PlanJpaEntity planEntity) {
        return Plan.builder()
                .id(planEntity.getId())
                .name(planEntity.getName())
                .description(planEntity.getDescription())
                .maxProjects(planEntity.getMaxProjects())
                .maxUsersPerProject(planEntity.getMaxUsersPerProject())
                .price(planEntity.getPrice())
                .active(planEntity.isActive())
                .createdAt(planEntity.getCreatedAt())
                .updatedAt(planEntity.getUpdatedAt())
                .build();
    }
}

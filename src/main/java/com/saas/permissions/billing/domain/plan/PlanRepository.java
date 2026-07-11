package com.saas.permissions.billing.domain.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    List<Plan> findAll();

    Optional<Plan> findById(UUID planId);

    Optional<Plan> findByName(String planName);

    Plan save(Plan plan);
}

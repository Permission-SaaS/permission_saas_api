package com.saas.permissions.project.infrastructure.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaProjectRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    List<ProjectJpaEntity> findByDeletedAtIsNullOrderByCreatedAtAsc();

    List<ProjectJpaEntity> findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<ProjectJpaEntity> findByClientIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID clientId);

    Optional<ProjectJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdAndDeletedAtIsNull(UUID id);
}

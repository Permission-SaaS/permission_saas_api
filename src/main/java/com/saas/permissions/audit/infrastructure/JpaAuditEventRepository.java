package com.saas.permissions.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAuditEventRepository extends JpaRepository<AuditEventJpaEntity, UUID> {

    List<AuditEventJpaEntity> findAllByOrderByOccurredAtDesc();

    List<AuditEventJpaEntity> findByProjectIdOrderByOccurredAtDesc(UUID projectId);
}

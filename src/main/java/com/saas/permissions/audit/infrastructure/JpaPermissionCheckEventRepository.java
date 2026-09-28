package com.saas.permissions.audit.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaPermissionCheckEventRepository extends JpaRepository<PermissionCheckEventJpaEntity, UUID> {

    @Query("""
            SELECT event FROM PermissionCheckEventJpaEntity event
            WHERE event.granted = false
                AND (:projectId IS NULL OR event.projectId = :projectId)
                AND (cast(:from as OffsetDateTime) IS NULL OR event.occurredAt >= :from)
                AND (cast(:to as OffsetDateTime) IS NULL OR event.occurredAt <= :to)
            ORDER BY event.occurredAt DESC
            """)
    List<PermissionCheckEventJpaEntity> searchDenied(UUID projectId, OffsetDateTime from, OffsetDateTime to);

}

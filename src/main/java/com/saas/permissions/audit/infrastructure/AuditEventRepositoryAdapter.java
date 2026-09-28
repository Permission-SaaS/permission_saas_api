package com.saas.permissions.audit.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.ProjectLifecycleEvent;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final JpaAuditEventRepository jpaAuditEventRepository;
    private final JpaPermissionCheckEventRepository permissionCheckEventRepository;

    @Override
    public AuditEvent save(AuditEvent event) {
        return toDomain(jpaAuditEventRepository.save(toJpa(event)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> search(String type, UUID projectId, OffsetDateTime from, OffsetDateTime to) {
        return jpaAuditEventRepository.search(type, projectId, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> searchDenied(UUID projectId, OffsetDateTime from, OffsetDateTime to) {
        return permissionCheckEventRepository.searchDenied(projectId, from, to).stream().map(this::toDomain).toList();
    }

    private AuditEventJpaEntity toJpa(AuditEvent event) {
        if (event instanceof PermissionCheckEvent check) {
            return PermissionCheckEventJpaEntity.builder()
                    .id(check.getId())
                    .projectId(check.getProjectId())
                    .occurredAt(check.getOccurredAt())
                    .routePath(check.getRoutePath())
                    .httpMethod(check.getHttpMethod())
                    .roleName(check.getRoleName())
                    .granted(check.isGranted())
                    .reason(check.getReason())
                    .durationMs(check.getDurationMs())
                    .ipAddress(check.getIpAddress())
                    .country(check.getCountry())
                    .build();
        }

        if (event instanceof ProjectLifecycleEvent lifecycle) {
            return ProjectLifecycleEventJpaEntity.builder()
                    .id(lifecycle.getId())
                    .projectId(lifecycle.getProjectId())
                    .occurredAt(lifecycle.getOccurredAt())
                    .action(lifecycle.getAction())
                    .projectName(lifecycle.getProjectName())
                    .performedBy(lifecycle.getPerformedBy())
                    .build();
        }

        throw new UnsupportedAuditEventException(event.getClass());
    }

    private AuditEvent toDomain(AuditEventJpaEntity entity) {
        if (entity instanceof PermissionCheckEventJpaEntity check) {
            return PermissionCheckEvent.builder()
                    .id(check.getId())
                    .projectId(check.getProjectId())
                    .occurredAt(check.getOccurredAt())
                    .routePath(check.getRoutePath())
                    .httpMethod(check.getHttpMethod())
                    .roleName(check.getRoleName())
                    .granted(check.isGranted())
                    .reason(check.getReason())
                    .durationMs(check.getDurationMs())
                    .ipAddress(check.getIpAddress())
                    .country(check.getCountry())
                    .build();
        }

        if (entity instanceof ProjectLifecycleEventJpaEntity lifecycle) {
            return ProjectLifecycleEvent.builder()
                    .id(lifecycle.getId())
                    .projectId(lifecycle.getProjectId())
                    .occurredAt(lifecycle.getOccurredAt())
                    .action(lifecycle.getAction())
                    .projectName(lifecycle.getProjectName())
                    .performedBy(lifecycle.getPerformedBy())
                    .build();
        }

        throw new UnsupportedAuditEventException(entity.getClass());
    }
}

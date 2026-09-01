package com.saas.permissions.audit.domain;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findAll();

    List<AuditEvent> findAllByProjectId(UUID projectId);
}

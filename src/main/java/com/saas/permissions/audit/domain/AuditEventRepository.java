package com.saas.permissions.audit.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> search(String type, UUID projectId, OffsetDateTime from, OffsetDateTime to);

    List<AuditEvent> searchDenied(UUID projectId, OffsetDateTime from, OffsetDateTime to);
}

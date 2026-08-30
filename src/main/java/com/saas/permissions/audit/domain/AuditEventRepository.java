package com.saas.permissions.audit.domain;

import java.util.List;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findAll();
}

package com.saas.permissions.audit.domain;

public interface AuditEventJournal {

    void record(AuditEvent event);
}

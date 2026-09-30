package com.saas.permissions.audit.domain;

public interface AuditTrail {
    void record(PermissionCheckEvent event);
}

package com.saas.permissions.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionCheckEvent extends AuditEvent {

    private String routePath;
    private String httpMethod;
    private String roleName;

    private boolean granted;

    private String reason;

    private double durationMs;

    private String ipAddress;
    private String country;

    @Override
    public String describe() {
        return (granted ? "PERMITIDO" : "NEGADO")
                + " " + httpMethod + " " + routePath
                + " para o cargo '" + roleName + "'"
                + (granted ? "" : " — " + reason);
    }

    @Override
    public String type() {
        return "PERMISSION_CHECK";
    }
}

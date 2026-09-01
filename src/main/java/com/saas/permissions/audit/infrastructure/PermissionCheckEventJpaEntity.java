package com.saas.permissions.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("PERMISSION_CHECK")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
class PermissionCheckEventJpaEntity extends AuditEventJpaEntity {

    @Column(name = "route_path", length = 255)
    private String routePath;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "role_name", length = 80)
    private String roleName;

    @Column(name = "granted")
    private boolean granted;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "duration_ms")
    private double durationMs;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "country", length = 60)
    private String country;
}

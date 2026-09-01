package com.saas.permissions.audit.infrastructure;

import java.util.UUID;

import com.saas.permissions.audit.domain.LifecycleAction;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("PROJECT_LIFECYCLE")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
class ProjectLifecycleEventJpaEntity extends AuditEventJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private LifecycleAction action;

    @Column(name = "project_name", length = 120)
    private String projectName;

    @Column(name = "performed_by")
    private UUID performedBy;
}

package com.saas.permissions.audit.domain;

import java.util.UUID;

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
public class ProjectLifecycleEvent extends AuditEvent {

    private LifecycleAction action;

    private String projectName;

    private UUID performedBy;

    @Override
    public String describe() {
        return "Projeto '" + projectName + "' " + action.getLabel()
                + " pelo cliente " + performedBy;
    }

    @Override
    public String type() {
        return "PROJECT_LIFECYCLE";
    }
}

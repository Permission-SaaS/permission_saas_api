package com.saas.permissions.audit.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public abstract class AuditEvent {

    private UUID id;

    private UUID projectId;

    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    public abstract String describe();

    public abstract String type();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", occurredAt=" + occurredAt +
                ", description='" + describe() + '\'' +
                '}';
    }
}

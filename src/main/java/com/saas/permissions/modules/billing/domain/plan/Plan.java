package com.saas.permissions.modules.billing.domain.plan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Plan {

    private UUID id;
    private String name;
    private String description;
    private int maxProjects;
    private int maxUsersPerProject;
    private BigDecimal price;

    @Builder.Default
    private boolean active = true;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

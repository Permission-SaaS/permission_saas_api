package com.saas.permissions.billing.domain.subscription;

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
public class ApiKey {

    private UUID id;
    private UUID subscriptionId;
    private String keyHash;

    private String plainKey;

    @Builder.Default
    private boolean active = true;

    private OffsetDateTime createdAt;
    private OffsetDateTime revokedAt;

    public void revoke() {
        this.active = false;
        this.revokedAt = OffsetDateTime.now();
    }
}

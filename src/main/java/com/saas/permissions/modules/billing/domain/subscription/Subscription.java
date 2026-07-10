package com.saas.permissions.modules.billing.domain.subscription;

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
public class Subscription {

    private UUID id;
    private UUID clientId;
    private UUID planId;

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.pending;

    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static Subscription pendingFor(UUID clientId, UUID planId) {
        return Subscription.builder()
                .clientId(clientId)
                .planId(planId)
                .status(SubscriptionStatus.pending)
                .build();
    }

    public void activate(OffsetDateTime startsAt, OffsetDateTime expiresAt) {
        this.status = SubscriptionStatus.active;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public void reject() {
        this.status = SubscriptionStatus.canceled;
    }

    public void expire() {
        this.status = SubscriptionStatus.expired;
    }

    public boolean isCurrentlyActive(OffsetDateTime now) {
        return status == SubscriptionStatus.active && expiresAt != null && expiresAt.isAfter(now);
    }
}

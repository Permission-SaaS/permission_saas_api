package com.saas.permissions.modules.billing.api.subscription.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SubscribeToPlanRequest(

        @NotNull UUID clientId,

        @NotNull UUID planId) {
}

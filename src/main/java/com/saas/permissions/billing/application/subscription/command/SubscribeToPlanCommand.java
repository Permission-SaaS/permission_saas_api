package com.saas.permissions.billing.application.subscription.command;

import java.util.UUID;

public record SubscribeToPlanCommand(
                UUID clientId,
                UUID planId) {

}
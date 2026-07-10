package com.saas.permissions.modules.billing.api.subscription.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.billing.api.subscription.dto.SubscribeToPlanRequest;
import com.saas.permissions.modules.billing.application.subscription.command.SubscribeToPlanCommand;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class SubscribeToPlanMapper implements Mapper<SubscribeToPlanRequest, SubscribeToPlanCommand> {

    @Override
    public SubscribeToPlanCommand map(SubscribeToPlanRequest request) {
        return new SubscribeToPlanCommand(request.clientId(), request.planId());
    }
}

package com.saas.permissions.billing.api.plan.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.billing.api.plan.dto.RegisterPlanRequest;
import com.saas.permissions.billing.application.plan.command.RegisterPlanCommand;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class RegisterPlanMapper implements Mapper<RegisterPlanRequest, RegisterPlanCommand> {

    @Override
    public RegisterPlanCommand map(RegisterPlanRequest request) {
        return new RegisterPlanCommand(
                request.name(),
                request.description(),
                request.maxProjects(),
                request.maxUsersPerProject(),
                request.price());
    }

}

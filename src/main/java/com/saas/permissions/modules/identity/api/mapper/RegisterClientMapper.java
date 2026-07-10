package com.saas.permissions.identity.api.mapper;

import com.saas.permissions.identity.api.dto.RegisterClientRequest;
import com.saas.permissions.identity.application.command.RegisterClientCommand;
import com.saas.permissions.shared.domain.Mapper;
import org.springframework.stereotype.Component;

@Component
public class RegisterClientMapper implements Mapper<RegisterClientRequest, RegisterClientCommand> {

    @Override
    public RegisterClientCommand map(RegisterClientRequest request) {
        return new RegisterClientCommand(
                request.name(),
                request.email(),
                request.phone(),
                request.rawPassword(),
                request.provider(),
                request.providerId()
        );
    }
}

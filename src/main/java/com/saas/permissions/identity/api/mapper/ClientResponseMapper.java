package com.saas.permissions.identity.api.mapper;

import com.saas.permissions.identity.api.dto.ClientResponse;
import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.shared.domain.Mapper;
import org.springframework.stereotype.Component;

@Component
public class ClientResponseMapper implements Mapper<Client, ClientResponse> {

    @Override
    public ClientResponse map(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getProvider() != null ? client.getProvider().name() : null,
                client.getProviderId(),
                client.getStatus().name(),
                client.isEmailVerified(),
                client.isBlocked(),
                client.getLoginAttempts(),
                client.getBlockExpiresAt() != null ? client.getBlockExpiresAt().toString() : null,
                client.getEmailVerifiedAt() != null ? client.getEmailVerifiedAt().toString() : null,
                client.getLastLoginAt() != null ? client.getLastLoginAt().toString() : null,
                client.getCreatedAt() != null ? client.getCreatedAt().toString() : null,
                client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : null,
                client.getDeletedAt() != null ? client.getDeletedAt().toString() : null
        );
    }
}

package com.saas.permissions.identity.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.identity.domain.ClientRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepository {

    private final JpaClientRepository jpa;

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Client save(Client client) {
        ClientJpaEntity saved = jpa.save(toJpa(client));
        return toDomain(saved);
    }

    private ClientJpaEntity toJpa(Client client) {
        return ClientJpaEntity.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .passwordHash(client.getPasswordHash())
                .provider(client.getProvider())
                .providerId(client.getProviderId())
                .status(client.getStatus())
                .emailVerified(client.isEmailVerified())
                .blocked(client.isBlocked())
                .loginAttempts(client.getLoginAttempts())
                .blockExpiresAt(client.getBlockExpiresAt())
                .emailVerifiedAt(client.getEmailVerifiedAt())
                .lastLoginAt(client.getLastLoginAt())
                .build();
    }

    private Client toDomain(ClientJpaEntity clientEntity) {
        return Client.builder()
                .id(clientEntity.getId())
                .name(clientEntity.getName())
                .email(clientEntity.getEmail())
                .phone(clientEntity.getPhone())
                .passwordHash(clientEntity.getPasswordHash())
                .provider(clientEntity.getProvider())
                .providerId(clientEntity.getProviderId())
                .status(clientEntity.getStatus())
                .emailVerified(clientEntity.isEmailVerified())
                .blocked(clientEntity.isBlocked())
                .loginAttempts(clientEntity.getLoginAttempts())
                .blockExpiresAt(clientEntity.getBlockExpiresAt())
                .emailVerifiedAt(clientEntity.getEmailVerifiedAt())
                .lastLoginAt(clientEntity.getLastLoginAt())
                .createdAt(clientEntity.getCreatedAt())
                .updatedAt(clientEntity.getUpdatedAt())
                .deletedAt(clientEntity.getDeletedAt())
                .build();
    }
}
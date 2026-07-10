package com.saas.permissions.modules.identity.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.modules.identity.domain.Client;
import com.saas.permissions.modules.identity.domain.ClientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindClientByIdUseCase {

    private final ClientRepository clientRepository;

    public Client execute(UUID clientId) {
        Optional<Client> foundClient = clientRepository.findById(clientId);

        if (foundClient.isEmpty()) {
            throw new RuntimeException("Client not found");
        }

        return foundClient.get();
    }

}

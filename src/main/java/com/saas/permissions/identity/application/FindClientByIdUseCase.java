package com.saas.permissions.identity.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.identity.domain.ClientRepository;
import com.saas.permissions.identity.domain.exception.ClientNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindClientByIdUseCase {

    private final ClientRepository clientRepository;

    public Client execute(UUID clientId) {
        Optional<Client> foundClient = clientRepository.findById(clientId);

        if (foundClient.isEmpty()) {
            throw new ClientNotFoundException();
        }

        return foundClient.get();
    }

}

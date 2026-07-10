package com.saas.permissions.identity.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.identity.domain.ClientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindAllClientsUseCase {

    private final ClientRepository clientRepository;

    public List<Client> execute() {
        return clientRepository.findAll();
    }
}

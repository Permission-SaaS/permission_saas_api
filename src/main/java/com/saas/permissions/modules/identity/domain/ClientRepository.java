package com.saas.permissions.modules.identity.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {

    boolean existsByEmail(String email);

    boolean existsById(UUID id);

    Optional<Client> findByEmail(String email);

    Optional<Client> findById(UUID id);

    List<Client> findAll();

    Client save(Client client);
}
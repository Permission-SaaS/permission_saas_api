package com.saas.permissions.identity.domain;

import java.util.Optional;

public interface ClientRepository {

    boolean existsByEmail(String email);

    Optional<Client> findByEmail(String email);

    Client save(Client client);
}
package com.saas.permissions.identity.domain.exception;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class ClientNotFoundException extends ResourceNotFoundException {
    public ClientNotFoundException() {
        super("Client not found");
    }
}

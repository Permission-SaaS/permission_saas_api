package com.saas.permissions.identity.application;

import com.saas.permissions.identity.application.command.RegisterClientCommand;
import com.saas.permissions.identity.domain.AuthProvider;
import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.identity.domain.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterClientUseCase {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public Client execute(RegisterClientCommand command) {

        if (clientRepository.existsByEmail(command.email())) {
            throw new IllegalStateException("Email already in use: " + command.email());
        }

        String passwordHash = (command.rawPassword() != null)
                ? passwordEncoder.encode(command.rawPassword())
                : null;

        Client client = Client.builder()
                .name(command.name())
                .email(command.email())
                .phone(command.phone())
                .passwordHash(passwordHash)
                .provider(command.provider() != null ? command.provider() : AuthProvider.local)
                .providerId(command.providerId())
                .build();

        return clientRepository.save(client);
    }
}

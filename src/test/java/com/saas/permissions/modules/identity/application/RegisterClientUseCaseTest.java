package com.saas.permissions.modules.identity.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.saas.permissions.modules.identity.application.command.RegisterClientCommand;
import com.saas.permissions.modules.identity.domain.AuthProvider;
import com.saas.permissions.modules.identity.domain.Client;
import com.saas.permissions.modules.identity.domain.ClientRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterClientUseCaseTest")
public class RegisterClientUseCaseTest {

    @Mock
     private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterClientUseCase useCase;

    private RegisterClientCommand validCommand() {
        return new RegisterClientCommand(
            "Ana Silva", "ana@email.com", "11999990000",
            "senha123",null, null
        );
    }

    @Test
    @DisplayName("should register a new client successfully")
    public void testRegisterClientSuccessfully() {
        when(clientRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashedPassword");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client registeredClient = useCase.execute(validCommand());

        assertTrue(registeredClient.getName().equals("Ana Silva"));
        assertTrue(registeredClient.getEmail().equals("ana@email.com"));
        assertTrue(registeredClient.getPhone().equals("11999990000"));
        assertTrue(registeredClient.getPasswordHash().equals("hashedPassword"));
        assertTrue(registeredClient.getProvider() == AuthProvider.local);
        assertTrue(registeredClient.getProviderId() == null);
    }



}

package com.saas.permissions.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.saas.permissions.identity.domain.AuthProvider;
import com.saas.permissions.identity.domain.Client;
import com.saas.permissions.identity.domain.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(ClientRepositoryAdapter.class)
@DisplayName("ClientRepositoryAdapter (IT)")
class ClientRepositoryAdapterIT {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("deve salvar e recuperar um cliente pelo email")
    void shouldSaveAndFindByEmail() {
        var client = Client.builder()
                .name("Maria")
                .email("maria@email.com")
                .provider(AuthProvider.local)
                .build();

        clientRepository.save(client);

        var found = clientRepository.findByEmail("maria@email.com");

        assertThat(found).isPresent();
        assertThat(found).hasValueSatisfying(savedClient -> {
            assertThat(savedClient.getId()).isNotNull();
            assertThat(savedClient.getName()).isEqualTo("Maria");
            assertThat(savedClient.getEmail()).isEqualTo("maria@email.com");
            assertThat(savedClient.getProvider()).isEqualTo(AuthProvider.local);
        });
    }
}
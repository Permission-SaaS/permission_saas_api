package com.saas.permissions.modules.identity.api;

import com.saas.permissions.modules.identity.api.dto.ClientResponse;
import com.saas.permissions.modules.identity.api.dto.RegisterClientRequest;
import com.saas.permissions.modules.identity.api.mapper.ClientResponseMapper;
import com.saas.permissions.modules.identity.api.mapper.RegisterClientMapper;
import com.saas.permissions.modules.identity.application.FindAllClientsUseCase;
import com.saas.permissions.modules.identity.application.FindClientByIdUseCase;
import com.saas.permissions.modules.identity.application.RegisterClientUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final RegisterClientUseCase registerClientUseCase;
    private final FindClientByIdUseCase findClientByIdUseCase;
    private final FindAllClientsUseCase findAllClientsUseCase;
    private final RegisterClientMapper registerClientMapper;
    private final ClientResponseMapper clientResponseMapper;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        var clients = findAllClientsUseCase.execute();
        return ResponseEntity.ok(clients.stream().map(clientResponseMapper::map).toList());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID clientId) {
        var client = findClientByIdUseCase.execute(clientId);
        return ResponseEntity.ok(clientResponseMapper.map(client));
    }

    @PostMapping("/register")
    public ResponseEntity<ClientResponse> register(@RequestBody @Valid RegisterClientRequest request) {
        var client = registerClientUseCase.execute(registerClientMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponseMapper.map(client));
    }
}

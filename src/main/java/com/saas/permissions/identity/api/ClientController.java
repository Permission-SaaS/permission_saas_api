package com.saas.permissions.identity.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.identity.api.dto.ClientResponse;
import com.saas.permissions.identity.api.dto.RegisterClientRequest;
import com.saas.permissions.identity.api.mapper.ClientResponseMapper;
import com.saas.permissions.identity.api.mapper.RegisterClientMapper;
import com.saas.permissions.identity.application.FindAllClientsUseCase;
import com.saas.permissions.identity.application.FindClientByIdUseCase;
import com.saas.permissions.identity.application.RegisterClientUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Client", description = "Gerenciamento de clientes")
public class ClientController {

    private final RegisterClientUseCase registerClientUseCase;
    private final FindClientByIdUseCase findClientByIdUseCase;
    private final FindAllClientsUseCase findAllClientsUseCase;
    private final RegisterClientMapper registerClientMapper;
    private final ClientResponseMapper clientResponseMapper;

    @GetMapping
    @Operation(summary = "Retorna todos os clientes cadastrados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        var clients = findAllClientsUseCase.execute();
        return ResponseEntity.ok(clients.stream().map(clientResponseMapper::map).toList());
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Retorna um cliente pelo UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID clientId) {
        var client = findClientByIdUseCase.execute(clientId);
        return ResponseEntity.ok(clientResponseMapper.map(client));
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ClientResponse> register(@RequestBody @Valid RegisterClientRequest request) {
        var client = registerClientUseCase.execute(registerClientMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponseMapper.map(client));
    }
}

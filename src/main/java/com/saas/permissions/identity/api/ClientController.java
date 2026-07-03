package com.saas.permissions.identity.api;

import com.saas.permissions.identity.api.dto.ClientResponse;
import com.saas.permissions.identity.api.dto.RegisterClientRequest;
import com.saas.permissions.identity.api.mapper.ClientResponseMapper;
import com.saas.permissions.identity.api.mapper.RegisterClientMapper;
import com.saas.permissions.identity.application.RegisterClientUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final RegisterClientUseCase registerClientUseCase;
    private final RegisterClientMapper registerClientMapper;
    private final ClientResponseMapper clientResponseMapper;

    @PostMapping("/register")
    public ResponseEntity<ClientResponse> register(@RequestBody @Valid RegisterClientRequest request) {
        var client = registerClientUseCase.execute(registerClientMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponseMapper.map(client));
    }
}

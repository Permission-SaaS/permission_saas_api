package com.saas.permissions.audit.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.audit.api.dto.AuditEventResponse;
import com.saas.permissions.audit.api.dto.SearchAuditEventsRequest;
import com.saas.permissions.audit.api.mapper.AuditEventResponseMapper;
import com.saas.permissions.audit.api.mapper.SearchAuditEventsMapper;
import com.saas.permissions.audit.application.SearchAuditEventsUseCase;
import com.saas.permissions.audit.domain.AuditTrailEntry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Trilha de auditoria das validacoes de permissao")
public class AuditEventController {

    private final SearchAuditEventsUseCase searchAuditEventsUseCase;

    private final SearchAuditEventsMapper searchAuditEventsMapper;

    private final AuditEventResponseMapper auditEventResponseMapper;

    @GetMapping
    @Operation(summary = "Lista a trilha de auditoria, do evento mais recente para o mais antigo. Repassa a consulta ao audit-service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtro invalido ou periodo invertido"),
            @ApiResponse(responseCode = "503", description = "audit-service fora do ar ou sem responder")
    })
    public ResponseEntity<List<AuditEventResponse>> searchAuditEvents(@Valid SearchAuditEventsRequest request) {

        List<AuditTrailEntry> events = searchAuditEventsUseCase.execute(searchAuditEventsMapper.map(request));

        return ResponseEntity.ok(events.stream().map(auditEventResponseMapper::map).toList());
    }
}

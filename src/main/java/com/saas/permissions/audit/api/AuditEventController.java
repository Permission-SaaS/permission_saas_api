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
import com.saas.permissions.audit.application.FindAuditEventsUseCase;
import com.saas.permissions.audit.domain.AuditEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Trilha de auditoria das validacoes de permissao")
public class AuditEventController {

    private final FindAuditEventsUseCase findAuditEventsUseCase;

    private final SearchAuditEventsMapper searchAuditEventsMapper;

    private final AuditEventResponseMapper auditEventResponseMapper;

    @GetMapping
    @Operation(summary = "Lista a trilha de auditoria, do evento mais recente para o mais antigo")
    @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso")
    public ResponseEntity<List<AuditEventResponse>> searchAuditEvents(@Valid SearchAuditEventsRequest request) {

        List<AuditEvent> events = findAuditEventsUseCase.execute(searchAuditEventsMapper.map(request));

        return ResponseEntity.ok(events.stream().map(auditEventResponseMapper::map).toList());
    }
}

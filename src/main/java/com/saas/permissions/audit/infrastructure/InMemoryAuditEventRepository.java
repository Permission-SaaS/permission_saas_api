package com.saas.permissions.audit.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;

/**
 * Banco simulado da trilha de auditoria (etapas 2-3). Substituido pelo adapter
 * JPA na etapa 4.
 */
@Repository
public class InMemoryAuditEventRepository implements AuditEventRepository {

    private final Map<UUID, AuditEvent> events = new ConcurrentHashMap<>();

    @Override
    public AuditEvent save(AuditEvent event) {
        ensureEvent(event);

        // O identificador nasce aqui, e nao no dominio, para respeitar o
        // ADR-001: na etapa 4 quem gera o id passa a ser o banco, e o dominio
        // nao muda.
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }

        events.put(event.getId(), event);

        return event;
    }

    @Override
    public List<AuditEvent> findAll() {
        return List.copyOf(events.values());
    }

    private void ensureEvent(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event cannot be null");
        }
    }
}

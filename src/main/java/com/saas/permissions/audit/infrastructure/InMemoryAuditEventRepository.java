package com.saas.permissions.audit.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;

@Repository
public class InMemoryAuditEventRepository implements AuditEventRepository {

    private final Map<UUID, AuditEvent> events = new ConcurrentHashMap<>();

    @Override
    public AuditEvent save(AuditEvent event) {
        ensureEvent(event);

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

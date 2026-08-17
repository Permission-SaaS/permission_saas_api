package com.saas.permissions.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.LifecycleAction;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.ProjectLifecycleEvent;

@Component
@Profile("demo")
@Order(2)
public class AuditDemoRunner implements CommandLineRunner {

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

    @Override
    public void run(String... args) {
        List<AuditEvent> events = List.of(
                ProjectLifecycleEvent.builder()
                        .id(UUID.randomUUID())
                        .projectId(PROJECT_ID)
                        .action(LifecycleAction.CREATED)
                        .projectName("Loja Virtual")
                        .performedBy(CLIENT_ID)
                        .build(),
                PermissionCheckEvent.builder()
                        .id(UUID.randomUUID())
                        .projectId(PROJECT_ID)
                        .routePath("/produtos")
                        .httpMethod("GET")
                        .roleName("VENDEDOR")
                        .granted(true)
                        .durationMs(12.47)
                        .ipAddress("189.45.12.7")
                        .country("BR")
                        .build(),
                PermissionCheckEvent.builder()
                        .id(UUID.randomUUID())
                        .projectId(PROJECT_ID)
                        .routePath("/produtos/{id}")
                        .httpMethod("DELETE")
                        .roleName("VENDEDOR")
                        .granted(false)
                        .reason("cargo sem permissao para remover produtos")
                        .durationMs(8.03)
                        .ipAddress("189.45.12.7")
                        .country("BR")
                        .build());

        // A lista e' de AuditEvent: type() e toString() resolvem para cada subclasse.
        for (AuditEvent event : events) {
            System.out.println("Audit event [" + event.type() + "]: " + event);
        }
    }
}

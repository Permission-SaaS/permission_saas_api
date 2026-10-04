package com.saas.permissions.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Liga o {@code @Async}, que roda o método numa thread do executor de tarefas do
 * Spring Boot em vez da thread de quem chamou. Hoje o usa o {@code AuditLogListener},
 * para a validação de permissão não esperar a publicação da auditoria.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

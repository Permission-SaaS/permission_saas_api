package com.saas.permissions.audit.infrastructure.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEventPublisher;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.exception.AuditEventNotPublishedException;
import com.saas.permissions.audit.infrastructure.messaging.dto.AuditMessage;
import com.saas.permissions.audit.infrastructure.messaging.dto.PermissionCheckPayload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RabbitAuditEventPublisher implements AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private final String source;

    private final String queue;

    public RabbitAuditEventPublisher(RabbitTemplate rabbitTemplate,
            @Value("${spring.application.name}") String source,
            @Value("${audit.messaging.queue}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.source = source;
        this.queue = queue;
    }

    @Override
    public void publish(PermissionCheckEvent event) {
        AuditMessage<PermissionCheckPayload> message = new AuditMessage<>(
                source,
                event.type(),
                event.getOccurredAt().toInstant(),
                new PermissionCheckPayload(
                        event.getProjectId(),
                        event.getRoutePath(),
                        event.getHttpMethod(),
                        event.getRoleName(),
                        event.isGranted(),
                        event.getReason(),
                        event.getDurationMs()));
        try {
            rabbitTemplate.convertAndSend(queue, message);
            log.info("Audit message published to {}: {} {}", queue, message.type(), event.describe());
        } catch (AmqpException e) {
            throw new AuditEventNotPublishedException(e);
        }
    }

}

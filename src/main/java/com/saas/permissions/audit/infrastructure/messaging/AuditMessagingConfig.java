package com.saas.permissions.audit.infrastructure.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AuditMessagingConfig {

    /**
     * O produtor também declara a fila: sem ela, uma mensagem publicada antes da
     * primeira subida do audit-service seria descartada pelo broker. A declaração
     * precisa ser idêntica à do audit-service, incluindo a fila de mensagens
     * mortas.
     */
    @Bean
    Queue auditEventsQueue(@Value("${audit.messaging.queue}") String queue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange("")
                .deadLetterRoutingKey(queue + ".dlq")
                .build();
    }

    @Bean
    MessageConverter auditMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

}

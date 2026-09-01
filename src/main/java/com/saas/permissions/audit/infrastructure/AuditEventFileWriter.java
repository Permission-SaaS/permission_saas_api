package com.saas.permissions.audit.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventJournal;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditEventFileWriter implements AuditEventJournal {

    private static final String SEPARATOR = ";";

    private final Path file;

    AuditEventFileWriter(@Value("${app.audit.journal-file:logs/audit-events.txt}") String file) {
        this.file = Path.of(file);
    }

    @Override
    public void record(AuditEvent event) {
        String line = String.join(SEPARATOR,
                event.type(),
                String.valueOf(event.getId()),
                String.valueOf(event.getProjectId()),
                String.valueOf(event.getOccurredAt()),
                event.describe());

        try {
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Nao foi possivel gravar o evento de auditoria em {}", file, e);
        }
    }
}

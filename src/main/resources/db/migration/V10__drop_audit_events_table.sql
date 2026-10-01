-- A trilha de auditoria saiu do monolito: desde 30/09/2026 ela é gravada e consultada no
-- audit-service, que tem o próprio banco (audit_db). A tabela criada na V8 não tem mais
-- quem a leia nem quem grave. Os índices e as constraints caem junto com ela.
-- As linhas antigas não são copiadas para o audit_db (ver ADR-010).
DROP TABLE audit_events;

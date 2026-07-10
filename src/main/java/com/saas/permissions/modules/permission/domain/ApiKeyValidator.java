package com.saas.permissions.modules.permission.domain;

/**
 * Porta consultada pelo {@link ApiKeyValidationHandler}. Mantém o domain de
 * {@code permission} livre de qualquer conhecimento sobre como uma ApiKey é
 * armazenada ou validada em {@code billing} (regra de ouro em
 * docs/ARCHITECTURE.md: domain não conhece outro módulo) — quem faz essa
 * ponte é o adapter em {@code permission/infrastructure}.
 */
public interface ApiKeyValidator {

    boolean isActive(String apiKey);
}

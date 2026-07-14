package com.saas.permissions.permission.infrastructure;

import org.springframework.stereotype.Component;

import com.saas.permissions.billing.application.subscription.FindActiveApiKeyByPlainKeyUseCase;
import com.saas.permissions.permission.domain.ApiKeyValidator;

import lombok.RequiredArgsConstructor;

/**
 * Implementa a porta {@link ApiKeyValidator} chamando o use case de
 * {@code billing} — comunicação entre módulos via use case, nunca via
 * repositório de outro módulo direto (docs/ARCHITECTURE.md).
 */
@Component
@RequiredArgsConstructor
public class BillingApiKeyValidator implements ApiKeyValidator {

    private final FindActiveApiKeyByPlainKeyUseCase findActiveApiKeyByPlainKeyUseCase;

    @Override
    public boolean isActive(String apiKey) {
        return findActiveApiKeyByPlainKeyUseCase.execute(apiKey).isPresent();
    }
}

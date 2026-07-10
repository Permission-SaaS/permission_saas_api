package com.saas.permissions.modules.permission.infrastructure;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.billing.application.subscription.FindApiKeyByPlainKeyUseCase;
import com.saas.permissions.modules.permission.domain.ApiKeyValidator;

import lombok.RequiredArgsConstructor;

/**
 * Implementa a porta {@link ApiKeyValidator} chamando o use case de
 * {@code billing} — comunicação entre módulos via use case, nunca via
 * repositório de outro módulo direto (docs/ARCHITECTURE.md).
 */
@Component
@RequiredArgsConstructor
public class BillingApiKeyValidator implements ApiKeyValidator {

    private final FindApiKeyByPlainKeyUseCase findApiKeyByPlainKeyUseCase;

    @Override
    public boolean isActive(String apiKey) {
        return findApiKeyByPlainKeyUseCase.execute(apiKey)
                .map(foundApiKey -> foundApiKey.isActive())
                .orElse(false);
    }
}

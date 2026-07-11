package com.saas.permissions.billing.infrastructure.subscription;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.saas.permissions.billing.domain.subscription.ApiKey;

/**
 * ConcreteCreator: gera a chave em texto puro como um UUID prefixado. Único
 * formato dentro do escopo do projeto (JWT está fora de escopo — ver
 * CLAUDE.md, Scope boundaries); novos formatos entram como novas subclasses
 * de {@link ApiKeyFactory}, sem alterar esta nem o Creator.
 */
@Component
public class UuidApiKeyFactory extends ApiKeyFactory {

    private static final String KEY_PREFIX = "sk_";

    public UuidApiKeyFactory(PasswordEncoder passwordEncoder) {
        super(passwordEncoder);
    }

    @Override
    protected ApiKey newApiKey(UUID subscriptionId) {
        String plainKey = KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");

        return ApiKey.builder()
                .subscriptionId(subscriptionId)
                .plainKey(plainKey)
                .active(true)
                .build();
    }
}

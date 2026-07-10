package com.saas.permissions.billing.infrastructure.subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.saas.permissions.billing.domain.subscription.ApiKey;

/**
 * Creator do Factory Method. Fixa o que toda ApiKey precisa ter antes de ser
 * entregue (hash + timestamp), e defere para a subclasse a decisão de como o
 * produto é montado ({@link #newApiKey(UUID)}). Classe abstrata de propósito:
 * o Spring não a registra como bean, evitando ambiguidade quando existir mais
 * de um ConcreteCreator (ex.: {@link UuidApiKeyFactory}).
 */
public abstract class ApiKeyFactory {

    protected final PasswordEncoder passwordEncoder;

    protected ApiKeyFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public final ApiKey create(UUID subscriptionId) {
        ApiKey apiKey = newApiKey(subscriptionId);
        apiKey.setKeyHash(passwordEncoder.encode(apiKey.getPlainKey()));
        apiKey.setCreatedAt(OffsetDateTime.now());
        return apiKey;
    }

    /**
     * Factory Method: a subclasse decide o formato da chave em texto puro e
     * monta o produto (sem hash/timestamp — isso é responsabilidade fixa de
     * {@link #create(UUID)}).
     */
    protected abstract ApiKey newApiKey(UUID subscriptionId);
}

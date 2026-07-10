package com.saas.permissions.modules.billing.application.subscription;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.saas.permissions.modules.billing.domain.subscription.ApiKey;
import com.saas.permissions.modules.billing.domain.subscription.ApiKeyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindApiKeyByPlainKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<ApiKey> execute(String plainKey) {
        return apiKeyRepository.findAllActive().stream()
                .filter(apiKey -> passwordEncoder.matches(plainKey, apiKey.getKeyHash()))
                .findFirst();
    }
}

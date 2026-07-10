package com.saas.permissions.modules.permission.domain;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiKeyValidationHandler extends PermissionValidationHandler {

    private final ApiKeyValidator apiKeyValidator;

    @Override
    protected PermissionCheckResult check(PermissionCheckRequest request) {
        if (!apiKeyValidator.isActive(request.apiKey())) {
            return PermissionCheckResult.deny("invalid or inactive api key");
        }

        return PermissionCheckResult.allow();
    }
}

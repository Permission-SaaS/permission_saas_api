package com.saas.permissions.modules.permission.application;

import org.springframework.stereotype.Service;

import com.saas.permissions.modules.permission.domain.ApiKeyValidationHandler;
import com.saas.permissions.modules.permission.domain.PermissionValidationHandler;
import com.saas.permissions.modules.permission.domain.RoleRouteValidationHandler;
import com.saas.permissions.modules.permission.domain.TokenValidationHandler;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;

@Service
public class ValidatePermissionUseCase {

    private final PermissionValidationHandler chain;

    public ValidatePermissionUseCase(
            ApiKeyValidationHandler apiKeyValidationHandler,
            TokenValidationHandler tokenValidationHandler,
            RoleRouteValidationHandler roleRouteValidationHandler) {
        apiKeyValidationHandler.linkWith(tokenValidationHandler)
                .linkWith(roleRouteValidationHandler);

        this.chain = apiKeyValidationHandler;
    }

    public PermissionCheckResult execute(PermissionCheckRequest request) {
        return chain.handle(request);
    }
}

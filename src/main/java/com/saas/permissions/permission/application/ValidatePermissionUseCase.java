package com.saas.permissions.permission.application;

import java.time.OffsetDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.saas.permissions.permission.domain.ApiKeyValidationHandler;
import com.saas.permissions.permission.domain.PermissionValidationHandler;
import com.saas.permissions.permission.domain.RoleRouteValidationHandler;
import com.saas.permissions.permission.domain.TokenValidationHandler;
import com.saas.permissions.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.permission.domain.dto.PermissionCheckResult;
import com.saas.permissions.permission.domain.event.PermissionValidatedEvent;

@Service
public class ValidatePermissionUseCase {

    private final PermissionValidationHandler chain;

    private final ApplicationEventPublisher eventPublisher;

    public ValidatePermissionUseCase(
            ApiKeyValidationHandler apiKeyValidationHandler,
            TokenValidationHandler tokenValidationHandler,
            RoleRouteValidationHandler roleRouteValidationHandler,
            ApplicationEventPublisher eventPublisher) {
        apiKeyValidationHandler.linkWith(tokenValidationHandler)
                .linkWith(roleRouteValidationHandler);

        this.chain = apiKeyValidationHandler;
        this.eventPublisher = eventPublisher;
    }

    public PermissionCheckResult execute(PermissionCheckRequest request) {
        long startedAt = System.nanoTime();

        PermissionCheckResult result = chain.handle(request);

        double durationMs = (System.nanoTime() - startedAt) / 1_000_000.0;

        eventPublisher.publishEvent(new PermissionValidatedEvent(
                request.role(),
                request.route(),
                result.granted(),
                result.reason(),
                durationMs,
                OffsetDateTime.now()));

        return result;
    }
}

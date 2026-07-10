package com.saas.permissions.modules.permission.domain;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;

/**
 * Deveria checar se {@link PermissionCheckRequest#role()} tem acesso à
 * {@link PermissionCheckRequest#route()} de um Project. Sempre concede porque
 * o módulo {@code project} (Role/Route/ProjectBuilder) ainda não foi
 * implementado por falta de tempo — ver docs/PLAN.md, "trabalho futuro". A
 * regra real entraria aqui sem tocar nos outros handlers da chain (OCP).
 */
@Component
public class RoleRouteValidationHandler extends PermissionValidationHandler {

    @Override
    protected PermissionCheckResult check(PermissionCheckRequest request) {
        return PermissionCheckResult.allow();
    }
}

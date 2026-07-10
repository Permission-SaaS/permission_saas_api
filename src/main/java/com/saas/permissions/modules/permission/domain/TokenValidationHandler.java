package com.saas.permissions.modules.permission.domain;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;

/**
 * Placeholder para um segundo fator de autenticação (ex.: token de sessão do
 * end user) além da ApiKey. Sempre concede — JWT/login está fora do escopo
 * do projeto (ver CLAUDE.md, Scope boundaries). Existe na chain para provar
 * que um novo handler se pluga sem alterar {@link ApiKeyValidationHandler}
 * nem {@link RoleRouteValidationHandler} (OCP).
 */
@Component
public class TokenValidationHandler extends PermissionValidationHandler {

    @Override
    protected PermissionCheckResult check(PermissionCheckRequest request) {
        return PermissionCheckResult.allow();
    }
}

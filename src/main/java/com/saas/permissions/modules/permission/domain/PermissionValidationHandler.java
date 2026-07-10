package com.saas.permissions.modules.permission.domain;

import com.saas.permissions.modules.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;

/**
 * Handler do Chain of Responsibility (GoF). Fixa o comportamento de
 * encadeamento — só repassa para o próximo handler se o atual conceder
 * permissão — e deixa a regra de cada concern ({@link #check}) para as
 * subclasses. Uma nova validação entra como um novo handler ligado à chain,
 * sem alterar os existentes (OCP).
 */
public abstract class PermissionValidationHandler {

    private PermissionValidationHandler next;

    public PermissionValidationHandler linkWith(PermissionValidationHandler next) {
        this.next = next;
        return next;
    }

    public final PermissionCheckResult handle(PermissionCheckRequest request) {
        PermissionCheckResult result = check(request);

        if (!result.granted() || next == null) {
            return result;
        }

        return next.handle(request);
    }

    protected abstract PermissionCheckResult check(PermissionCheckRequest request);
}

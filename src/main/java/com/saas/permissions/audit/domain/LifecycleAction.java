package com.saas.permissions.audit.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LifecycleAction {

    CREATED("criado"),
    UPDATED("atualizado"),
    DELETED("removido");

    private final String label;
}

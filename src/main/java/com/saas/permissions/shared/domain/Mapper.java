package com.saas.permissions.shared.domain;

public interface Mapper<I, O> {
    O map(I input);
}

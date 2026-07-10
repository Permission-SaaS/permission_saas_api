package com.saas.permissions.permission.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.permission.api.dto.ValidatePermissionRequest;
import com.saas.permissions.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class ValidatePermissionMapper implements Mapper<ValidatePermissionRequest, PermissionCheckRequest> {

    @Override
    public PermissionCheckRequest map(ValidatePermissionRequest request) {
        return new PermissionCheckRequest(request.apiKey(), request.role(), request.route());
    }
}

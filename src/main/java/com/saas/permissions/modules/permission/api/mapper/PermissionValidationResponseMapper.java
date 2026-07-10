package com.saas.permissions.modules.permission.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.permission.api.dto.PermissionValidationResponse;
import com.saas.permissions.modules.permission.domain.dto.PermissionCheckResult;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class PermissionValidationResponseMapper implements Mapper<PermissionCheckResult, PermissionValidationResponse> {

    @Override
    public PermissionValidationResponse map(PermissionCheckResult result) {
        return new PermissionValidationResponse(result.granted(), result.reason());
    }
}

package com.saas.permissions.modules.permission.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.modules.permission.api.dto.PermissionValidationResponse;
import com.saas.permissions.modules.permission.api.dto.ValidatePermissionRequest;
import com.saas.permissions.modules.permission.api.mapper.PermissionValidationResponseMapper;
import com.saas.permissions.modules.permission.api.mapper.ValidatePermissionMapper;
import com.saas.permissions.modules.permission.application.ValidatePermissionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PermissionController {

    private final ValidatePermissionUseCase validatePermissionUseCase;
    private final ValidatePermissionMapper validatePermissionMapper;
    private final PermissionValidationResponseMapper permissionValidationResponseMapper;

    @PostMapping("/validate-permission")
    public ResponseEntity<PermissionValidationResponse> validate(
            @RequestBody @Valid ValidatePermissionRequest request) {
        var result = validatePermissionUseCase.execute(validatePermissionMapper.map(request));
        return ResponseEntity.ok(permissionValidationResponseMapper.map(result));
    }
}

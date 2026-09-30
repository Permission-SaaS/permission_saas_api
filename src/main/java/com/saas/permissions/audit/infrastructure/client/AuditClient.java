package com.saas.permissions.audit.infrastructure.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.saas.permissions.audit.infrastructure.client.dto.AuditEventResponse;
import com.saas.permissions.audit.infrastructure.client.dto.RegisterPermissionCheckRequest;

@FeignClient(name = "audit-service", url = "${audit.service.url}")
public interface AuditClient {

    @PostMapping("/audit-events/permission-checks")
    AuditEventResponse registerPermissionCheck(@RequestBody RegisterPermissionCheckRequest request);

    @GetMapping("/audit-events")
    List<AuditEventResponse> searchAuditEvents(
            @RequestParam(name = "projectId", required = false) UUID projectId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "onlyDenied", required = false) Boolean onlyDenied,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to);
}

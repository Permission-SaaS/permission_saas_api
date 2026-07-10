package com.saas.permissions.billing.api.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.billing.api.subscription.dto.SubscribeToPlanRequest;
import com.saas.permissions.billing.api.subscription.dto.SubscriptionResponse;
import com.saas.permissions.billing.api.subscription.mapper.SubscribeToPlanMapper;
import com.saas.permissions.billing.api.subscription.mapper.SubscriptionResponseMapper;
import com.saas.permissions.billing.application.subscription.SubscribeToPlanUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscribeToPlanUseCase subscribeToPlanUseCase;
    private final SubscribeToPlanMapper subscribeToPlanMapper;
    private final SubscriptionResponseMapper subscriptionResponseMapper;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(@RequestBody @Valid SubscribeToPlanRequest request) {
        var result = subscribeToPlanUseCase.execute(subscribeToPlanMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionResponseMapper.map(result));
    }
}

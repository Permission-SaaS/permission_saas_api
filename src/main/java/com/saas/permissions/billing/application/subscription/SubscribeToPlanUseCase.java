package com.saas.permissions.billing.application.subscription;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.permissions.billing.application.subscription.command.SubscribeToPlanCommand;
import com.saas.permissions.billing.domain.subscription.ApiKey;
import com.saas.permissions.billing.domain.subscription.ApiKeyRepository;
import com.saas.permissions.billing.domain.subscription.PaymentGateway;
import com.saas.permissions.billing.domain.plan.Plan;
import com.saas.permissions.billing.domain.subscription.Subscription;
import com.saas.permissions.billing.domain.subscription.SubscriptionRepository;
import com.saas.permissions.billing.domain.subscription.SubscriptionStatus;
import com.saas.permissions.billing.domain.subscription.dto.PaymentRequest;
import com.saas.permissions.billing.domain.subscription.dto.PaymentResult;
import com.saas.permissions.billing.domain.subscription.dto.SubscriptionResult;
import com.saas.permissions.billing.infrastructure.subscription.ApiKeyFactory;
import com.saas.permissions.billing.application.plan.FindPlanByIdUseCase;
import com.saas.permissions.identity.application.FindClientByIdUseCase;
import com.saas.permissions.identity.domain.Client;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscribeToPlanUseCase {
    private final PaymentGateway paymentGateway;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyFactory apiKeyFactory;
    private final FindClientByIdUseCase findClientByIdUseCase;
    private final FindPlanByIdUseCase findPlanByIdUseCase;

    @Transactional
    public SubscriptionResult execute(SubscribeToPlanCommand command) {
        Client foundClient = findClientByIdUseCase.execute(command.clientId());
        Plan foundPlan = findPlanByIdUseCase.execute(command.planId());

        Optional<Subscription> existingSubscription = subscriptionRepository
                .findByClientIdAndStatus(foundClient.getId(), SubscriptionStatus.active);

        if (existingSubscription.isPresent()) {
            handleExistingSubscription(existingSubscription.get(), foundPlan.getId());
        }

        Subscription subscription = Subscription.pendingFor(foundClient.getId(), foundPlan.getId());
        subscription = subscriptionRepository.save(subscription);

        PaymentResult paymentResult = paymentGateway.process(
                new PaymentRequest(subscription.getId(), foundPlan.getPrice()));

        if (!paymentResult.approved()) {
            subscription.reject();
            subscriptionRepository.save(subscription);
            throw new RuntimeException("Payment declined for subscription " + subscription.getId());
        }

        subscription.activate(OffsetDateTime.now(), OffsetDateTime.now().plusMonths(1));
        subscription = subscriptionRepository.save(subscription);

        ApiKey apiKey = apiKeyFactory.create(subscription.getId());
        apiKeyRepository.save(apiKey);

        return new SubscriptionResult(subscription, apiKey);
    }

    private void handleExistingSubscription(Subscription existing, UUID newPlanId) {
        if (!existing.isCurrentlyActive(OffsetDateTime.now())) {
            existing.expire();
            retire(existing);
            return;
        }

        if (existing.getPlanId().equals(newPlanId)) {
            throw new RuntimeException(
                    "Client already has an active subscription to this plan, valid until " + existing.getExpiresAt());
        }

        existing.reject();
        retire(existing);
    }

    private void retire(Subscription subscription) {
        subscriptionRepository.save(subscription);
        Optional<ApiKey> apiKey = apiKeyRepository.findBySubscriptionId(subscription.getId());

        if (apiKey.isPresent()) {
            apiKey.get().revoke();
            apiKeyRepository.save(apiKey.get());
        }
    }
}

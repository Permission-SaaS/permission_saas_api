package com.saas.permissions.billing.infrastructure.subscription;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.saas.permissions.billing.domain.subscription.PaymentGateway;
import com.saas.permissions.billing.domain.subscription.dto.PaymentRequest;
import com.saas.permissions.billing.domain.subscription.dto.PaymentResult;
import com.saas.permissions.billing.infrastructure.subscription.ExternalPaymentGatewaySimulator.LegacyChargeResponse;

import lombok.RequiredArgsConstructor;

/**
 * Adapter do padrão Adapter (GoF): implementa o Target ({@link PaymentGateway})
 * e por baixo dos panos conversa com o Adaptee
 * ({@link ExternalPaymentGatewaySimulator}),
 * traduzindo entre os dois formatos.
 */
@Component
@RequiredArgsConstructor
public class FakePaymentGatewayAdapter implements PaymentGateway {

    private static final BigDecimal CENTS_PER_UNIT = BigDecimal.valueOf(100);
    private static final String APPROVED_STATUS = "OK";

    private final ExternalPaymentGatewaySimulator externalGateway;

    @Override
    public PaymentResult process(PaymentRequest request) {
        long amountInCents = request.amount().multiply(CENTS_PER_UNIT).longValueExact();
        String reference = request.subscriptionId().toString();

        LegacyChargeResponse response = externalGateway.charge(amountInCents, reference);

        boolean approved = APPROVED_STATUS.equals(response.statusCode());
        return new PaymentResult(approved, response.legacyTransactionRef());
    }
}

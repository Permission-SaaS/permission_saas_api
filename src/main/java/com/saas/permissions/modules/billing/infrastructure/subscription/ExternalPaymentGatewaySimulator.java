package com.saas.permissions.modules.billing.infrastructure.subscription;

import java.util.UUID;

/**
 * Adaptee do padrão Adapter (GoF): simula uma biblioteca de pagamento de
 * terceiros, com uma API que já "nasce pronta" e é incompatível com
 * {@code PaymentGateway} de propósito — nomes de método diferentes, valor em
 * centavos (não {@code BigDecimal}), retorno com vocabulário próprio
 * ("OK"/"FAIL" em vez de {@code boolean}). Isso é o que, na vida real, seria
 * o SDK de um provedor como Stripe/PagSeguro: você não controla esse formato,
 * só o adapta.
 */
public class ExternalPaymentGatewaySimulator {

    public LegacyChargeResponse charge(long amountInCents, String reference) {
        String legacyTransactionRef = "LEGACY-" + UUID.randomUUID();
        return new LegacyChargeResponse("OK", legacyTransactionRef);
    }

    public record LegacyChargeResponse(String statusCode, String legacyTransactionRef) {
    }
}

package com.saas.permissions.billing.domain.subscription;

import com.saas.permissions.billing.domain.subscription.dto.PaymentRequest;
import com.saas.permissions.billing.domain.subscription.dto.PaymentResult;

/**
 * Target do padrão Adapter (GoF). É a porta que o
 * {@code SubscribeToPlanUseCase}
 * conhece — ele nunca fala diretamente com um gateway de pagamento real ou
 * simulado, só com esta interface (DIP). Só tem um método (ISP): trocar de
 * provedor de pagamento no futuro não exige alterar quem consome.
 */
public interface PaymentGateway {

    PaymentResult process(PaymentRequest request);
}

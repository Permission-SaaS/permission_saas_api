package com.saas.permissions.billing.infrastructure.subscription;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link ExternalPaymentGatewaySimulator} representa um SDK de terceiro
 * (Adaptee do padrão Adapter) e por isso não é anotado com {@code @Component}
 * — em produção esse bean viria de uma lib externa, não do component scan.
 * Aqui ele é registrado manualmente para simular esse cenário.
 */
@Configuration
public class BillingConfig {

    @Bean
    public ExternalPaymentGatewaySimulator externalPaymentGatewaySimulator() {
        return new ExternalPaymentGatewaySimulator();
    }
}

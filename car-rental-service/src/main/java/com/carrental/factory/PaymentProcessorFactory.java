package com.carrental.factory;

import com.carrental.enums.PaymentMethod;
import com.carrental.strategy.PaymentStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentProcessorFactory – FACTORY PATTERN that resolves the correct PaymentStrategy
 * at runtime based on the PaymentMethod enum value.
 *
 * =============================================
 * Design Pattern: FACTORY PATTERN
 * =============================================
 * At startup, Spring injects ALL PaymentStrategy beans into the list.
 * @PostConstruct builds an EnumMap keyed by PaymentMethod → O(1) lookup.
 *
 * Adding a new payment method:
 *   1. Create a new @Component class implementing PaymentStrategy
 *   2. Return the new PaymentMethod from getMethod()
 *   3. Add the value to the PaymentMethod enum
 *   → Zero changes needed to this Factory or PaymentService
 *
 * This adheres to the Open/Closed Principle.
 */
@Component  // Spring-managed singleton – injected into PaymentService
@Slf4j
public class PaymentProcessorFactory {

    // Spring injects all PaymentStrategy beans found in the application context
    // This list will contain: CreditCardPaymentStrategy, UpiPaymentStrategy, WalletPaymentStrategy
    private final List<PaymentStrategy> strategies;

    // EnumMap provides O(1) lookup – faster than HashMap for enum keys
    private final Map<PaymentMethod, PaymentStrategy> strategyMap = new EnumMap<>(PaymentMethod.class);

    /**
     * Constructor injection of all PaymentStrategy implementations.
     * Spring auto-discovers all @Component beans implementing PaymentStrategy.
     *
     * @param strategies all payment strategy beans in the application context
     */
    public PaymentProcessorFactory(List<PaymentStrategy> strategies) {
        this.strategies = strategies;  // inject all concrete strategies
    }

    /**
     * Builds the lookup map once after Spring has injected all beans.
     * Each strategy registers itself by its PaymentMethod key.
     */
    @PostConstruct  // runs after dependency injection is complete
    private void init() {
        // iterate each concrete strategy and register it in the map
        for (PaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.getMethod(), strategy);  // key=PaymentMethod, value=strategy bean
            log.info("[PaymentFactory] Registered strategy: {} → {}",
                    strategy.getMethod(), strategy.getClass().getSimpleName());
        }
    }

    /**
     * Returns the PaymentStrategy for the given payment method.
     *
     * @param method the payment method chosen by the customer
     * @return the concrete PaymentStrategy implementation
     * @throws IllegalArgumentException if no strategy is registered for the method
     */
    public PaymentStrategy getProcessor(PaymentMethod method) {
        PaymentStrategy strategy = strategyMap.get(method);  // O(1) EnumMap lookup

        if (strategy == null) {
            // This should never happen if all PaymentMethod values have a corresponding strategy
            throw new IllegalArgumentException("No payment strategy registered for method: " + method);
        }

        log.debug("[PaymentFactory] Resolved strategy: {} → {}",
                method, strategy.getClass().getSimpleName());
        return strategy;  // return the concrete strategy for the caller to invoke
    }
}


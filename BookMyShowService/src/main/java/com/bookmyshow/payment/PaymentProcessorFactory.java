package com.bookmyshow.payment;

import com.bookmyshow.enums.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================
 * PaymentProcessorFactory – Factory Pattern
 * ============================================================
 *
 * DESIGN PATTERN: Factory Pattern
 *
 * Resolves the correct PaymentProcessor strategy implementation
 * based on the PaymentGateway enum value at runtime.
 *
 * HOW IT WORKS:
 * 1. Spring injects ALL PaymentProcessor beans into the constructor
 *    as a List<PaymentProcessor> (Spring automatically collects all
 *    beans implementing the interface).
 * 2. We build a Map<gatewayName, processor> from that list.
 * 3. getProcessor(PaymentGateway) looks up the map and returns the
 *    correct strategy in O(1) time.
 *
 * WHY Factory?
 * - Client (PaymentService) doesn't need to know concrete class names.
 * - Adding a new gateway (e.g. PhonePe) = add one @Component class
 *   and update PaymentGateway enum – zero changes to factory or service.
 * - Open/Closed Principle: open for extension, closed for modification.
 */
@Slf4j
@Component  // Spring singleton – created once, holds the processor map
public class PaymentProcessorFactory {

    /**
     * Map from gateway name (e.g. "RAZORPAY") → concrete processor.
     * Built at startup and reused for every payment request.
     */
    private final Map<String, PaymentProcessor> processorMap;

    /**
     * Spring injects all PaymentProcessor beans (Razorpay, Stripe, Paytm)
     * as a list. We convert to a map keyed by getGatewayName().
     *
     * @param processors All @Component classes implementing PaymentProcessor
     */
    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        // Stream: convert list → map for O(1) lookup
        this.processorMap = processors.stream()
                .collect(Collectors.toMap(
                        PaymentProcessor::getGatewayName,  // Key: gateway name string
                        Function.identity()                // Value: the processor itself
                ));

        // Log registered gateways at startup for observability
        log.info("PaymentProcessorFactory initialized with gateways: {}", processorMap.keySet());
    }

    /**
     * Returns the PaymentProcessor (Strategy) for the requested gateway.
     *
     * @param gateway The payment gateway enum value
     * @return Concrete PaymentProcessor strategy
     * @throws IllegalArgumentException if no processor found for gateway
     */
    public PaymentProcessor getProcessor(PaymentGateway gateway) {
        // Look up by enum name (e.g. RAZORPAY.name() = "RAZORPAY")
        PaymentProcessor processor = processorMap.get(gateway.name());

        // Guard: throw immediately if gateway is not configured
        if (processor == null) {
            throw new IllegalArgumentException(
                "No PaymentProcessor found for gateway: " + gateway.name() +
                ". Ensure a @Component implementing PaymentProcessor exists."
            );
        }

        log.debug("Resolved PaymentProcessor: {} for gateway: {}", processor.getClass().getSimpleName(), gateway);
        return processor;  // Return the correct strategy
    }
}


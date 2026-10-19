package com.carrental.strategy;

import com.carrental.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * WalletPaymentStrategy – STRATEGY PATTERN concrete implementation for in-app wallet payments.
 *
 * =============================================
 * Design Pattern: STRATEGY PATTERN (Concrete Strategy)
 * =============================================
 * The wallet is a pre-funded balance stored in the platform's own database.
 * This strategy deducts the rental amount from the user's wallet balance.
 *
 * In production: check wallet balance, atomically debit amount in a DB transaction.
 */
@Component  // Spring-managed bean – auto-discovered by PaymentProcessorFactory
@Slf4j
public class WalletPaymentStrategy implements PaymentStrategy {

    /**
     * Debit the rental amount from the user's in-app wallet.
     *
     * Production flow:
     * 1. Load wallet balance for the user
     * 2. Check sufficient funds (throw exception if not)
     * 3. Atomically debit amount using a DB transaction
     * 4. Record wallet transaction entry
     *
     * @param idempotencyKey rental reference to prevent duplicate debits
     * @param amount         amount to debit from wallet
     */
    @Override
    public PaymentResult process(String idempotencyKey, BigDecimal amount) {
        log.info("[WALLET] Processing wallet debit. idempotencyKey={}, amount={}", idempotencyKey, amount);

        // ── MOCK WALLET DEBIT ──────────────────────────────────────────────────────────
        // In production: walletService.debit(userId, amount, idempotencyKey)
        // This would check balance and atomically reduce it inside a @Transactional method
        String walletTxnId = "WLT-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[WALLET] Wallet debit successful. txnId={}", walletTxnId);
        return PaymentResult.success(walletTxnId, amount);  // return value object with txn reference
    }

    /**
     * Credit a refund back to the user's wallet (instant – no bank processing delay).
     *
     * @param transactionId original wallet debit transaction ID
     * @param amount        amount to credit back to wallet
     */
    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("[WALLET] Processing wallet refund. originalTxn={}, amount={}", transactionId, amount);

        // ── MOCK WALLET CREDIT ─────────────────────────────────────────────────────────
        // In production: walletService.credit(userId, amount, "REFUND-" + transactionId)
        String refundId = "WLT-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        log.info("[WALLET] Wallet refund successful. refundId={}", refundId);
        return PaymentResult.success(refundId, amount);
    }

    /**
     * Returns the PaymentMethod this strategy handles – used by PaymentProcessorFactory.
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.WALLET;  // factory maps WALLET → this bean
    }
}


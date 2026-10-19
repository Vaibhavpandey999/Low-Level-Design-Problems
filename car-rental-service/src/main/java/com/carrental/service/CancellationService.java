package com.carrental.service;

import com.carrental.enums.RentalStatus;
import com.carrental.exception.InvalidRentalStateException;
import com.carrental.exception.ResourceNotFoundException;
import com.carrental.model.CancellationPolicy;
import com.carrental.model.Rental;
import com.carrental.repository.jpa.CancellationPolicyRepository;
import com.carrental.repository.jpa.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * CancellationService – computes the cancellation penalty and updates rental status.
 *
 * Cancellation policy rules (from CancellationPolicy table):
 *   - Different car categories have different penalty tiers
 *   - The policy with the smallest hoursBeforePickup threshold that still applies is used
 *   - If no policy applies (cancellation is far in advance), no penalty is charged
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationService {

    private final RentalRepository rentalRepository;                        // REPOSITORY PATTERN
    private final CancellationPolicyRepository cancellationPolicyRepository; // REPOSITORY PATTERN

    /**
     * Process a rental cancellation.
     *
     * @param rentalId rental to cancel
     * @param reason   cancellation reason from the user
     * @return CancellationResult with penalty amount and refund amount
     */
    @Transactional
    public CancellationResult processCancel(Long rentalId, String reason) {
        log.info("[CancellationService] Cancelling rental. id={}", rentalId);

        // ── Step 1: Load and validate rental state ─────────────────────────────────────
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found: " + rentalId));

        // Only PENDING or ACTIVE rentals can be cancelled
        if (rental.getStatus() == RentalStatus.COMPLETED || rental.getStatus() == RentalStatus.CANCELLED) {
            throw new InvalidRentalStateException("Cannot cancel a " + rental.getStatus() + " rental.");
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 2: Calculate hours until pickup ───────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        // convert LocalDate startDate to LocalDateTime at midnight for hour calculation
        LocalDateTime pickupDateTime = rental.getStartDate().atStartOfDay();
        long hoursUntilPickup = ChronoUnit.HOURS.between(now, pickupDateTime);
        log.info("[CancellationService] Hours until pickup: {}", hoursUntilPickup);
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 3: Determine applicable cancellation policy ───────────────────────────
        BigDecimal penaltyPercent = BigDecimal.ZERO;  // default: no penalty
        List<CancellationPolicy> policies = cancellationPolicyRepository
                .findByCategoryType(rental.getCar().getCategory().getType());

        if (!policies.isEmpty() && hoursUntilPickup >= 0) {
            // Find the most restrictive policy that applies (smallest threshold ≤ hoursUntilPickup)
            penaltyPercent = policies.stream()
                    .filter(p -> hoursUntilPickup <= p.getHoursBeforePickup()) // policy applies if within threshold
                    .max(Comparator.comparing(CancellationPolicy::getPenaltyPercent)) // pick highest penalty
                    .map(CancellationPolicy::getPenaltyPercent)
                    .orElse(BigDecimal.ZERO);  // no matching policy = no penalty
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 4: Compute penalty and refund amounts ─────────────────────────────────
        BigDecimal totalAmount = rental.getTotalAmount();                // original charge
        BigDecimal penaltyAmount = totalAmount
                .multiply(penaltyPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); // penaltyPercent% of total
        BigDecimal refundAmount = totalAmount.subtract(penaltyAmount);   // refund = total - penalty
        log.info("[CancellationService] Penalty={}%, amount={}, refund={}", penaltyPercent, penaltyAmount, refundAmount);
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 5: Update rental status ───────────────────────────────────────────────
        rental.setStatus(RentalStatus.CANCELLED);  // advance state machine to terminal state
        rentalRepository.save(rental);             // persist cancellation
        // ──────────────────────────────────────────────────────────────────────────────

        return new CancellationResult(rentalId, penaltyAmount, refundAmount, reason);
    }

    /**
     * Value object returned by processCancel().
     * Contains all data needed by RentalFacade to trigger refund and publish event.
     */
    public record CancellationResult(
            Long rentalId,           // cancelled rental ID
            BigDecimal penaltyAmount, // amount withheld as penalty
            BigDecimal refundAmount,  // amount returned to customer
            String reason             // cancellation reason for event payload
    ) {}
}


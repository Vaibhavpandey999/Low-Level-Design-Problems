package com.carrental.service;

import com.carrental.enums.CarStatus;
import com.carrental.enums.RentalStatus;
import com.carrental.exception.CarNotAvailableException;
import com.carrental.model.Branch;
import com.carrental.model.Car;
import com.carrental.model.CarCategory;
import com.carrental.model.Rental;
import com.carrental.model.User;
import com.carrental.repository.jpa.BranchRepository;
import com.carrental.repository.jpa.CarRepository;
import com.carrental.repository.jpa.RentalRepository;
import com.carrental.enums.CarCategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RentalServiceTest – unit tests for RentalService.
 *
 * Uses Mockito to mock all dependencies (repositories) so tests run
 * without a real database. Tests verify:
 * - Happy path booking creates a PENDING rental
 * - Unavailable car throws CarNotAvailableException (prevents double-booking)
 * - Invalid date range throws IllegalArgumentException
 */
@ExtendWith(MockitoExtension.class)       // enables Mockito annotations without Spring context
@DisplayName("RentalService Unit Tests")
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;   // mock – no real DB

    @Mock
    private CarRepository carRepository;          // mock – controls car status in tests

    @Mock
    private BranchRepository branchRepository;    // mock – provides branch data

    @InjectMocks
    private RentalService rentalService;          // the class under test – mocks injected

    // Test fixtures
    private User testUser;
    private Car availableCar;
    private Car rentedCar;
    private Branch testBranch;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phone("+91-9999999999")
                .licenceNumber("DL-1234567890")
                .build();

        // Set up car category
        CarCategory economy = CarCategory.builder()
                .id(1L)
                .type(CarCategoryType.ECONOMY)
                .baseDailyRate(new BigDecimal("999.00"))
                .build();

        // Set up an available car
        availableCar = Car.builder()
                .id(10L)
                .make("Maruti")
                .model("Alto")
                .year(2022)
                .licensePlate("MH01AB1234")
                .status(CarStatus.AVAILABLE)     // ← AVAILABLE – booking should succeed
                .dailyRate(new BigDecimal("899.00"))
                .category(economy)
                .version(0L)                     // initial optimistic lock version
                .build();

        // Set up a rented car (should reject booking)
        rentedCar = Car.builder()
                .id(11L)
                .make("Honda")
                .model("City")
                .status(CarStatus.RENTED)         // ← RENTED – booking should fail
                .dailyRate(new BigDecimal("1299.00"))
                .category(economy)
                .version(2L)
                .build();

        // Set up test branch
        testBranch = Branch.builder()
                .id(1L)
                .name("Mumbai Airport")
                .city("Mumbai")
                .build();
    }

    /**
     * Happy path: booking an available car creates a PENDING rental with correct total amount.
     */
    @Test
    @DisplayName("Should create PENDING rental for available car")
    void bookCar_availableCar_createsPendingRental() {
        // Arrange: mock repository responses
        when(carRepository.findByIdWithOptimisticLock(10L)).thenReturn(Optional.of(availableCar));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> {
            Rental saved = inv.getArgument(0);
            saved = Rental.builder()                   // simulate DB saving with generated ID
                    .id(100L)
                    .user(saved.getUser())
                    .car(saved.getCar())
                    .pickupBranch(saved.getPickupBranch())
                    .dropoffBranch(saved.getDropoffBranch())
                    .startDate(saved.getStartDate())
                    .endDate(saved.getEndDate())
                    .status(RentalStatus.PENDING)
                    .totalAmount(saved.getTotalAmount())
                    .rentalReference(saved.getRentalReference())
                    .build();
            return saved;
        });

        LocalDate start = LocalDate.now().plusDays(1);   // tomorrow
        LocalDate end   = LocalDate.now().plusDays(4);   // 3 days later

        // Act
        Rental result = rentalService.createPendingRental(testUser, 10L, 1L, 1L, start, end);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RentalStatus.PENDING);          // must be PENDING
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2697.00");      // 899 × 3 days
        assertThat(result.getRentalReference()).startsWith("CR-");                // idempotency key format

        // Verify repository was called (behavior verification)
        verify(carRepository, times(1)).findByIdWithOptimisticLock(10L);
        verify(rentalRepository, times(1)).save(any(Rental.class));
    }

    /**
     * Negative path: booking a RENTED car must throw CarNotAvailableException.
     * This tests the double-booking prevention logic.
     */
    @Test
    @DisplayName("Should throw CarNotAvailableException for a RENTED car")
    void bookCar_rentedCar_throwsException() {
        // Arrange: car is RENTED
        when(carRepository.findByIdWithOptimisticLock(11L)).thenReturn(Optional.of(rentedCar));
        when(branchRepository.findById(anyLong())).thenReturn(Optional.of(testBranch));

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end   = LocalDate.now().plusDays(3);

        // Act + Assert: expect exception
        assertThatThrownBy(() ->
                rentalService.createPendingRental(testUser, 11L, 1L, 1L, start, end)
        )
        .isInstanceOf(CarNotAvailableException.class)            // correct exception type
        .hasMessageContaining("not available");                  // meaningful error message

        // Verify that save was NEVER called (no booking created for unavailable car)
        verify(rentalRepository, never()).save(any());
    }

    /**
     * Validation test: start date in the past must be rejected.
     */
    @Test
    @DisplayName("Should reject start date in the past")
    void bookCar_pastStartDate_throwsIllegalArgument() {
        LocalDate pastDate = LocalDate.now().minusDays(1);  // yesterday
        LocalDate future   = LocalDate.now().plusDays(3);

        assertThatThrownBy(() ->
                rentalService.createPendingRental(testUser, 10L, 1L, 1L, pastDate, future)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("past");

        verifyNoInteractions(carRepository);   // should fail before even hitting the DB
    }

    /**
     * Validation test: end date must be after start date.
     */
    @Test
    @DisplayName("Should reject endDate not after startDate")
    void bookCar_invalidDateRange_throwsIllegalArgument() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end   = LocalDate.now().plusDays(3);  // end BEFORE start

        assertThatThrownBy(() ->
                rentalService.createPendingRental(testUser, 10L, 1L, 1L, start, end)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("before");

        verifyNoInteractions(carRepository);   // date validation must happen before DB access
    }
}


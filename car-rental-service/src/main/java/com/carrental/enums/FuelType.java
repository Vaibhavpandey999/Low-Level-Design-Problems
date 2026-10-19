package com.carrental.enums;

/**
 * FuelType – the propulsion technology of a car.
 *
 * Used in search filters and displayed on car detail pages.
 * Affects the refuelling terms shown in the booking confirmation.
 */
public enum FuelType {
    PETROL,   // internal combustion – petrol/gasoline
    DIESEL,   // internal combustion – diesel
    ELECTRIC, // fully battery-electric vehicle (BEV)
    HYBRID    // petrol + electric (HEV / PHEV)
}


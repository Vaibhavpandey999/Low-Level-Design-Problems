package com.carrental.enums;

/**
 * CarCategoryType – classifies cars into pricing and comfort tiers.
 *
 * ECONOMY  : budget hatchbacks – cheapest daily rate
 * COMPACT  : mid-size sedans – moderate daily rate
 * SUV      : sport utility vehicles – premium daily rate
 * LUXURY   : high-end cars (BMW, Mercedes) – highest daily rate
 * VAN      : multi-passenger / cargo vans – flat commercial rate
 *
 * Used by CancellationPolicy to determine penalty percentage per category.
 */
public enum CarCategoryType {
    ECONOMY,   // e.g., Alto, WagonR – lowest price tier
    COMPACT,   // e.g., Dzire, Amaze – mid-range
    SUV,       // e.g., Creta, Seltos – premium
    LUXURY,    // e.g., BMW 5 Series, Mercedes E-Class
    VAN        // e.g., Innova Crysta, Transit – group or cargo transport
}


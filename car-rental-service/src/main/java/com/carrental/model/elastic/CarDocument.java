package com.carrental.model.elastic;

import com.carrental.enums.CarCategoryType;
import com.carrental.enums.CarStatus;
import com.carrental.enums.FuelType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * CarDocument – Elasticsearch document that mirrors the Car entity for full-text search.
 *
 * Why Elasticsearch instead of PostgreSQL for search?
 * - Full-text relevance ranking (inverted index) – "toyota camry" matches even partial tokens
 * - Fuzzy search handles typos: "toyata" → "toyota"
 * - Sub-50 ms latency at scale (vs PostgreSQL LIKE '%...%' which causes full table scans)
 * - Geo-point field for branch proximity filtering
 *
 * Sync Strategy: CarDocument is written asynchronously via Kafka consumer
 * (eventual consistency – search index lags DB by < 1 second, acceptable for search UX)
 *
 * Design Pattern: Builder (Lombok @Builder)
 */
@Document(indexName = "cars")     // Elasticsearch index name
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarDocument {

    @Id                                                  // Elasticsearch document ID
    private String id;                                   // same as Car.id (as String for ES)

    @Field(type = FieldType.Text, analyzer = "standard") // analysed – tokenised for full-text search
    private String make;                                 // e.g., "Toyota" → tokens: ["toyota"]

    @Field(type = FieldType.Text, analyzer = "standard") // analysed – supports partial matches
    private String model;                                // e.g., "Camry"

    @Field(type = FieldType.Integer)
    private Integer year;                                // manufacturing year (numeric filter)

    @Field(type = FieldType.Keyword)                     // keyword – exact match / aggregations
    private CarCategoryType categoryType;                // ECONOMY / SUV / LUXURY etc.

    @Field(type = FieldType.Keyword)
    private FuelType fuelType;                           // PETROL / ELECTRIC etc.

    @Field(type = FieldType.Keyword)
    private CarStatus status;                            // AVAILABLE / RENTED – filter out unavailable

    @Field(type = FieldType.Double)
    private BigDecimal dailyRate;                        // price range filter

    @Field(type = FieldType.Text, analyzer = "standard") // free-text features string
    private String features;                             // e.g., "GPS AC Bluetooth sunroof"

    @Field(type = FieldType.Keyword)
    private String branchCity;                           // filter by city (e.g., "Mumbai")

    @Field(type = FieldType.Long)
    private Long branchId;                               // branch ID for direct lookup

    @Field(type = FieldType.Double)
    private Double branchLatitude;                       // geo proximity search

    @Field(type = FieldType.Double)
    private Double branchLongitude;                      // geo proximity search
}


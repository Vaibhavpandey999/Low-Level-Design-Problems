package com.carrental.repository.elasticsearch;

import com.carrental.enums.CarCategoryType;
import com.carrental.enums.CarStatus;
import com.carrental.model.elastic.CarDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CarSearchRepository – Elasticsearch REPOSITORY PATTERN for car full-text search.
 *
 * Extends ElasticsearchRepository which provides CRUD and custom query methods
 * against the "cars" Elasticsearch index.
 *
 * Why Elasticsearch over PostgreSQL LIKE queries?
 * - Inverted index: O(log n) search vs O(n) full table scan
 * - Relevance ranking: results ordered by match score
 * - Fuzzy matching: handles typos automatically
 * - Analysed fields: tokenisation, stemming, lowercase normalisation
 */
@Repository
public interface CarSearchRepository extends ElasticsearchRepository<CarDocument, String> {

    /**
     * Find available cars in a specific city.
     * Used as the base query for the search endpoint (city + availability filter).
     *
     * @param status     CarStatus.AVAILABLE
     * @param branchCity e.g., "Mumbai"
     */
    List<CarDocument> findByStatusAndBranchCity(CarStatus status, String branchCity);

    /**
     * Find available cars in a city filtered by category type.
     *
     * @param status       CarStatus.AVAILABLE
     * @param branchCity   city name
     * @param categoryType e.g., SUV
     */
    List<CarDocument> findByStatusAndBranchCityAndCategoryType(
            CarStatus status, String branchCity, CarCategoryType categoryType);

    /**
     * Full-text search across make, model, and features fields.
     * Uses Elasticsearch @Query with multi_match for relevance-ranked results.
     *
     * @param keyword search term (e.g., "toyota suv gps")
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"make\", \"model\", \"features\"], \"fuzziness\": \"AUTO\"}}")
    List<CarDocument> fullTextSearch(String keyword);
}


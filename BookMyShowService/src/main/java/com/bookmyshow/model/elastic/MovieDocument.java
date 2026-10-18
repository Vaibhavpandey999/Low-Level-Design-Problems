package com.bookmyshow.model.elastic;

import com.bookmyshow.enums.Genre;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * ============================================================
 * MovieDocument – Elasticsearch Index Document
 * ============================================================
 *
 * A denormalized read-model of Movie stored in Elasticsearch.
 * Enables full-text search on title, description, and cast,
 * plus keyword filtering on genre, language, city.
 *
 * This is a SEPARATE document from the Movie JPA entity.
 * It is updated asynchronously via Kafka when a Movie is
 * created/updated (Event-Driven architecture).
 *
 * Why Elasticsearch instead of PostgreSQL LIKE queries?
 * - ES uses inverted indexes → sub-millisecond full-text search
 * - Supports fuzzy matching (typo tolerance)
 * - Supports relevance scoring and faceting (aggregations)
 * - Decouples read path from write path (CQRS principle)
 *
 * DESIGN PATTERN: CQRS (Command Query Responsibility Segregation) –
 *   writes go to PostgreSQL, reads for search go to Elasticsearch.
 */
@Document(indexName = "movies")   // ES index name
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDocument {

    @Id                           // ES document ID (mapped to Movie.id as String)
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")  // Analyzed: supports partial & fuzzy match
    private String title;         // Movie title – full-text searchable

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;   // Synopsis – full-text searchable

    @Field(type = FieldType.Keyword)  // Keyword: exact match / aggregation only
    private String genre;         // Genre enum value as string

    @Field(type = FieldType.Keyword)
    private String language;      // e.g. "English" – exact filter

    @Field(type = FieldType.Keyword)
    private String certification; // "U", "UA", "A"

    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> cast;    // Actor names – full-text searchable

    @Field(type = FieldType.Double)
    private Double rating;        // Numeric – range filter (e.g. rating > 7.0)

    @Field(type = FieldType.Keyword)
    private String posterUrl;     // Not searched but included in response

    @Field(type = FieldType.Integer)
    private Integer durationMinutes;  // Duration filter
}


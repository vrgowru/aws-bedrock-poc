package com.example.ragapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

// Request DTO
public record QueryRequest(
    @NotBlank(message = "Question cannot be blank")
    @Size(max = 1000, message = "Question must be less than 1000 characters")
    String question,
    
    @Size(max = 10, message = "Maximum 10 filters allowed")
    List<SearchFilter> filters,
    
    Integer maxResults,
    
    Double threshold
) {
    public QueryRequest {
        if (maxResults == null) {
            maxResults = 5;
        }
        if (maxResults < 1 || maxResults > 20) {
            maxResults = 5;
        }
        if (threshold == null) {
            threshold = 0.7;
        }
    }
}

// Search filter for metadata filtering
public record SearchFilter(
    @NotBlank String field,
    @NotBlank String value,
    FilterOperator operator
) {
    public SearchFilter {
        if (operator == null) {
            operator = FilterOperator.EQUALS;
        }
    }
}

public enum FilterOperator {
    EQUALS,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN
}

// Response DTO
public record QueryResponse(
    String answer,
    List<RetrievedDocument> sources,
    double confidence,
    long processingTimeMs
) {}

// Retrieved document from vector search
public record RetrievedDocument(
    String id,
    String content,
    double score,
    Map<String, Object> metadata
) {
    public static RetrievedDocument fromSpringAiDocument(Document document, double score) {
        return new RetrievedDocument(
            document.getId(),
            document.getContent(),
            score,
            document.getMetadata()
        );
    }
}
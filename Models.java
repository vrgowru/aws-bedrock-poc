package com.example.ragapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.util.List;
import java.util.Map;

// ============= REQUEST/RESPONSE DTOs =============

/**
 * Request DTO for RAG queries
 */
public record QueryRequest(
    @NotBlank(message = "Question cannot be blank")
    @Size(max = 1000, message = "Question must be less than 1000 characters")
    String question,
    
    @Size(max = 10, message = "Maximum 10 filters allowed")
    List<SearchFilter> filters,
    
    @Min(value = 1, message = "Max results must be at least 1")
    @Max(value = 20, message = "Max results cannot exceed 20")
    Integer maxResults,
    
    @DecimalMin(value = "0.0", message = "Threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Threshold must be between 0.0 and 1.0")
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
        if (threshold < 0.0 || threshold > 1.0) {
            threshold = 0.7;
        }
    }
}

/**
 * Response DTO for RAG queries
 */
public record QueryResponse(
    String answer,
    List<RetrievedDocument> sources,
    double confidence,
    long processingTimeMs,
    QueryMetadata metadata
) {
    public static QueryResponse success(String answer, List<RetrievedDocument> sources, 
                                      double confidence, long processingTime) {
        return new QueryResponse(answer, sources, confidence, processingTime, 
            new QueryMetadata(sources.size(), "success", null));
    }
    
    public static QueryResponse error(String errorMessage, long processingTime) {
        return new QueryResponse(
            "I encountered an error while processing your question. Please try again later.",
            List.of(), 0.0, processingTime,
            new QueryMetadata(0, "error", errorMessage)
        );
    }
}

/**
 * Metadata for query responses
 */
public record QueryMetadata(
    int documentsFound,
    String status,
    String errorMessage
) {}

// ============= SEARCH MODELS =============

/**
 * Search filter for metadata-based filtering
 */
public record SearchFilter(
    @NotBlank(message = "Field name cannot be blank")
    String field,
    
    @NotBlank(message = "Filter value cannot be blank")
    String value,
    
    FilterOperator operator
) {
    public SearchFilter {
        if (operator == null) {
            operator = FilterOperator.EQUALS;
        }
    }
}

/**
 * Filter operators for search
 */
public enum FilterOperator {
    EQUALS,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    NOT_EQUALS,
    IN,
    NOT_IN
}

/**
 * Retrieved document from vector search
 */
public record RetrievedDocument(
    String id,
    String content,
    double score,
    Map<String, Object> metadata
) {
    /**
     * Create from LangChain4j TextSegment with score
     */
    public static RetrievedDocument from(String id, String content, double score, Map<String, Object> metadata) {
        return new RetrievedDocument(id, content, score, metadata);
    }
    
    /**
     * Get metadata value by key with default
     */
    public String getMetadataString(String key, String defaultValue) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Get metadata value as integer
     */
    public Integer getMetadataInt(String key, Integer defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if document has specific tag
     */
    @SuppressWarnings("unchecked")
    public boolean hasTag(String tag) {
        Object tags = metadata.get("tags");
        if (tags instanceof List) {
            return ((List<String>) tags).contains(tag);
        }
        return false;
    }
}

// ============= DOCUMENT MANAGEMENT DTOs =============

/**
 * Request for indexing a single document
 */
public record IndexDocumentRequest(
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 50000, message = "Content must be less than 50000 characters")
    String content,
    
    Map<String, Object> metadata
) {}

/**
 * Response for document indexing
 */
public record IndexDocumentResponse(
    String documentId,
    String status,
    String message,
    long processingTimeMs
) {
    public static IndexDocumentResponse success(String documentId, long processingTime) {
        return new IndexDocumentResponse(documentId, "success", 
            "Document indexed successfully", processingTime);
    }
    
    public static IndexDocumentResponse error(String errorMessage, long processingTime) {
        return new IndexDocumentResponse(null, "error", errorMessage, processingTime);
    }
}

/**
 * Document DTO for batch operations
 */
public record DocumentDto(
    String id,
    
    @NotBlank(message = "Content cannot be blank")
    String content,
    
    Map<String, Object> metadata
) {}

/**
 * Request for batch document indexing
 */
public record BatchIndexRequest(
    @Size(min = 1, max = 100, message = "Must provide between 1 and 100 documents")
    List<DocumentDto> documents
) {}

/**
 * Response for batch document indexing
 */
public record BatchIndexResponse(
    int totalDocuments,
    int successfulDocuments,
    int failedDocuments,
    List<String> documentIds,
    List<String> errors,
    long processingTimeMs
) {
    public static BatchIndexResponse success(int total, List<String> documentIds, long processingTime) {
        return new BatchIndexResponse(total, total, 0, documentIds, List.of(), processingTime);
    }
    
    public static BatchIndexResponse partial(int total, int successful, List<String> documentIds, 
                                           List<String> errors, long processingTime) {
        return new BatchIndexResponse(total, successful, total - successful, 
            documentIds, errors, processingTime);
    }
}

/**
 * Request for deleting documents
 */
public record DeleteDocumentsRequest(
    @Size(min = 1, max = 100, message = "Must provide between 1 and 100 document IDs")
    List<String> documentIds
) {}

/**
 * Response for document deletion
 */
public record DeleteDocumentsResponse(
    int totalRequested,
    int successfulDeletions,
    int failedDeletions,
    List<String> errors,
    long processingTimeMs
) {
    public static DeleteDocumentsResponse success(int total, long processingTime) {
        return new DeleteDocumentsResponse(total, total, 0, List.of(), processingTime);
    }
    
    public static DeleteDocumentsResponse partial(int total, int successful, 
                                                List<String> errors, long processingTime) {
        return new DeleteDocumentsResponse(total, successful, total - successful, 
            errors, processingTime);
    }
}

// ============= VECTOR SEARCH MODELS =============

/**
 * Vector search request
 */
public record VectorSearchRequest(
    List<Float> queryEmbedding,
    
    @Min(value = 1, message = "Max results must be at least 1")
    @Max(value = 50, message = "Max results cannot exceed 50")
    int maxResults,
    
    @DecimalMin(value = "0.0", message = "Threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Threshold must be between 0.0 and 1.0")
    double threshold,
    
    List<SearchFilter> filters
) {
    public VectorSearchRequest {
        if (maxResults < 1 || maxResults > 50) {
            maxResults = 5;
        }
        if (threshold < 0.0 || threshold > 1.0) {
            threshold = 0.7;
        }
    }
}

/**
 * Vector search response
 */
public record VectorSearchResponse(
    List<RetrievedDocument> documents,
    int totalFound,
    double maxScore,
    double minScore,
    long searchTimeMs
) {}

// ============= SYSTEM MODELS =============

/**
 * Health check response
 */
public record HealthResponse(
    String status,
    String service,
    String version,
    long timestamp,
    Map<String, ComponentHealth> components
) {}

/**
 * Component health status
 */
public record ComponentHealth(
    String status,
    String message,
    Map<String, Object> details
) {
    public static ComponentHealth up(String message) {
        return new ComponentHealth("UP", message, Map.of());
    }
    
    public static ComponentHealth down(String message, Map<String, Object> details) {
        return new ComponentHealth("DOWN", message, details);
    }
}

/**
 * API information response
 */
public record ApiInfoResponse(
    String name,
    String version,
    String description,
    Map<String, String> endpoints,
    Map<String, Object> configuration
) {}

/**
 * Error response for API errors
 */
public record ErrorResponse(
    long timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors
) {
    public static ErrorResponse create(int status, String error, String message, String path) {
        return new ErrorResponse(System.currentTimeMillis(), status, error, message, path, Map.of());
    }
    
    public static ErrorResponse withValidationErrors(int status, String error, String message, 
                                                   String path, Map<String, String> validationErrors) {
        return new ErrorResponse(System.currentTimeMillis(), status, error, message, path, validationErrors);
    }
}

// ============= CONFIGURATION MODELS =============

/**
 * Embedding model configuration
 */
public record EmbeddingModelConfiguration(
    String modelId,
    int dimension,
    int maxInputTokens,
    double defaultThreshold,
    int timeoutSeconds
) {}

/**
 * Chat model configuration
 */
public record ChatModelConfiguration(
    String modelId,
    double temperature,
    int maxTokens,
    int timeoutSeconds,
    List<String> stopSequences
) {}

/**
 * Vector store configuration
 */
public record VectorStoreConfiguration(
    String storeType,
    String endpoint,
    String indexName,
    int dimension,
    String similarityMetric,
    Map<String, Object> settings
) {}

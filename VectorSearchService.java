package com.example.ragapi.service;

import com.example.ragapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.opensearch.OpenSearchVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);
    
    private final OpenSearchVectorStore vectorStore;
    
    public VectorSearchService(OpenSearchVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    public List<RetrievedDocument> searchSimilarDocuments(
            String query, 
            int maxResults,
            List<SearchFilter> filters,
            double threshold) {
        
        try {
            logger.debug("Searching for similar documents with {} filters", 
                    filters != null ? filters.size() : 0);
            
            // Build search request
            SearchRequest.Builder requestBuilder = SearchRequest.query(query)
                    .withTopK(maxResults)
                    .withSimilarityThreshold(threshold);
            
            // Add filters if provided
            if (filters != null && !filters.isEmpty()) {
                Filter filter = buildFilter(filters);
                requestBuilder.withFilterExpression(filter);
            }
            
            SearchRequest searchRequest = requestBuilder.build();
            
            // Execute search
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            logger.debug("Found {} documents", results.size());
            
            return results.stream()
                    .map(doc -> RetrievedDocument.fromSpringAiDocument(doc, getDocumentScore(doc)))
                    .toList();
            
        } catch (Exception e) {
            logger.error("Error searching documents", e);
            throw new RuntimeException("Failed to search documents", e);
        }
    }
    
    private Filter buildFilter(List<SearchFilter> filters) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        
        if (filters.size() == 1) {
            return buildSingleFilter(builder, filters.get(0));
        }
        
        // For multiple filters, combine with AND
        Filter combinedFilter = buildSingleFilter(builder, filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            Filter nextFilter = buildSingleFilter(builder, filters.get(i));
            combinedFilter = builder.and(combinedFilter, nextFilter).build();
        }
        
        return combinedFilter;
    }
    
    private Filter buildSingleFilter(FilterExpressionBuilder builder, SearchFilter filter) {
        String fieldName = filter.field();
        String value = filter.value();
        
        return switch (filter.operator()) {
            case EQUALS -> builder.eq(fieldName, value).build();
            case CONTAINS -> builder.in(fieldName, value).build();
            case GREATER_THAN -> builder.gt(fieldName, value).build();
            case LESS_THAN -> builder.lt(fieldName, value).build();
        };
    }
    
    private double getDocumentScore(Document document) {
        // Spring AI Document doesn't expose similarity score directly
        // You might need to extract it from metadata if available
        Object score = document.getMetadata().get("score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        // Default score if not available
        return 1.0;
    }
    
    public void addDocuments(List<Document> documents) {
        try {
            logger.debug("Adding {} documents to vector store", documents.size());
            vectorStore.add(documents);
            logger.info("Successfully added {} documents", documents.size());
        } catch (Exception e) {
            logger.error("Error adding documents to vector store", e);
            throw new RuntimeException("Failed to add documents", e);
        }
    }
    
    public void deleteDocuments(List<String> documentIds) {
        try {
            logger.debug("Deleting {} documents from vector store", documentIds.size());
            vectorStore.delete(documentIds);
            logger.info("Successfully deleted {} documents", documentIds.size());
        } catch (Exception e) {
            logger.error("Error deleting documents from vector store", e);
            throw new RuntimeException("Failed to delete documents", e);
        }
    }
}d("metadata." + filter.field())
                    .value(filter.value())
            )));
            case CONTAINS -> Query.of(q -> q.wildcard(w -> w
                    .field("metadata." + filter.field())
                    .value("*" + filter.value() + "*")
            ));
            case GREATER_THAN -> Query.of(q -> q.range(r -> r
                    .field("metadata." + filter.field())
                    .gt(com.fasterxml.jackson.databind.JsonNode.valueOf(filter.value()))
            ));
            case LESS_THAN -> Query.of(q -> q.range(r -> r
                    .field("metadata." + filter.field())
                    .lt(com.fasterxml.jackson.databind.JsonNode.valueOf(filter.value()))
            ));
        };
    }
    
    @SuppressWarnings("unchecked")
    private RetrievedDocument mapHitToDocument(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        if (source == null) {
            source = Map.of();
        }
        
        String content = (String) source.get(contentField);
        Map<String, Object> metadataMap = (Map<String, Object>) source.get("metadata");
        
        DocumentMetadata metadata = null;
        if (metadataMap != null) {
            metadata = new DocumentMetadata(
                    (String) metadataMap.get("title"),
                    (String) metadataMap.get("author"),
                    (String) metadataMap.get("source"),
                    (String) metadataMap.get("createdAt"),
                    (List<String>) metadataMap.get("tags")
            );
        }
        
        return new RetrievedDocument(
                hit.id(),
                content != null ? content : "",
                hit.score() != null ? hit.score() : 0.0,
                metadata
        );
    }
}
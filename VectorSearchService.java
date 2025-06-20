package com.example.ragapi.service;

import com.example.ragapi.model.*;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);
    
    private final OpenSearchEmbeddingStore embeddingStore;
    private final EmbeddingService embeddingService;
    
    @Value("${rag.retrieval.default-results:5}")
    private int defaultMaxResults;
    
    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double defaultThreshold;

    public VectorSearchService(OpenSearchEmbeddingStore embeddingStore, EmbeddingService embeddingService) {
        this.embeddingStore = embeddingStore;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Search for similar documents using text query
     */
    public List<RetrievedDocument> searchSimilarDocuments(
            String query, 
            int maxResults,
            List<SearchFilter> filters,
            double threshold) {
        
        try {
            logger.debug("Searching for similar documents with query: '{}', maxResults: {}, threshold: {}", 
                    query.substring(0, Math.min(50, query.length())), maxResults, threshold);
            
            // Generate embedding for the query
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
            
            // Search using embedding
            return searchByEmbedding(queryEmbedding, maxResults, filters, threshold);
            
        } catch (Exception e) {
            logger.error("Error searching documents with query: {}", query, e);
            throw new RuntimeException("Failed to search documents", e);
        }
    }
    
    /**
     * Search for similar documents using pre-computed embedding
     */
    public List<RetrievedDocument> searchByEmbedding(
            List<Float> queryEmbedding,
            int maxResults,
            List<SearchFilter> filters,
            double threshold) {
        
        try {
            logger.debug("Searching by embedding with {} dimensions, maxResults: {}, threshold: {}", 
                    queryEmbedding.size(), maxResults, threshold);
            
            // Create embedding object
            Embedding embedding = new Embedding(queryEmbedding);
            
            // Build search request
            EmbeddingSearchRequest.Builder requestBuilder = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(maxResults)
                    .minScore(threshold);
            
            // Add metadata filters if provided
            if (filters != null && !filters.isEmpty()) {
                Metadata metadataFilter = buildMetadataFilter(filters);
                requestBuilder.filter(metadataFilter);
            }
            
            EmbeddingSearchRequest searchRequest = requestBuilder.build();
            
            // Execute search
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            logger.debug("Found {} documents", searchResult.matches().size());
            
            // Convert results to our domain model
            return searchResult.matches().stream()
                    .map(this::convertToRetrievedDocument)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error searching by embedding", e);
            throw new RuntimeException("Failed to search by embedding", e);
        }
    }
    
    /**
     * Add documents to the vector store
     */
    public void addDocuments(List<DocumentForIndexing> documents) {
        try {
            logger.info("Adding {} documents to vector store", documents.size());
            
            List<TextSegment> segments = new ArrayList<>();
            List<Embedding> embeddings = new ArrayList<>();
            
            for (DocumentForIndexing doc : documents) {
                // Create text segment with metadata
                Metadata metadata = new Metadata();
                if (doc.metadata() != null) {
                    doc.metadata().forEach(metadata::put);
                }
                
                TextSegment segment = TextSegment.from(doc.content(), metadata);
                segments.add(segment);
                
                // Generate embedding
                List<Float> embedding = embeddingService.generateEmbedding(doc.content());
                embeddings.add(new Embedding(embedding));
            }
            
            // Add to store
            embeddingStore.addAll(embeddings, segments);
            
            logger.info("Successfully added {} documents to vector store", documents.size());
            
        } catch (Exception e) {
            logger.error("Error adding documents to vector store", e);
            throw new RuntimeException("Failed to add documents", e);
        }
    }
    
    /**
     * Add a single document to the vector store
     */
    public String addDocument(String content, Map<String, Object> metadata) {
        try {
            String documentId = UUID.randomUUID().toString();
            
            // Create metadata with ID
            Metadata segmentMetadata = new Metadata();
            segmentMetadata.put("id", documentId);
            if (metadata != null) {
                metadata.forEach(segmentMetadata::put);
            }
            
            // Create text segment
            TextSegment segment = TextSegment.from(content, segmentMetadata);
            
            // Generate embedding
            List<Float> embedding = embeddingService.generateEmbedding(content);
            
            // Add to store
            String storeId = embeddingStore.add(new Embedding(embedding), segment);
            
            logger.info("Added document with ID: {}, store ID: {}", documentId, storeId);
            return documentId;
            
        } catch (Exception e) {
            logger.error("Error adding single document", e);
            throw new RuntimeException("Failed to add document", e);
        }
    }
    
    /**
     * Delete documents by IDs
     */
    public void deleteDocuments(List<String> documentIds) {
        try {
            logger.info("Deleting {} documents", documentIds.size());
            embeddingStore.removeAll(documentIds);
            logger.info("Successfully deleted {} documents", documentIds.size());
        } catch (Exception e) {
            logger.error("Error deleting documents", e);
            throw new RuntimeException("Failed to delete documents", e);
        }
    }
    
    /**
     * Get similar documents for a given document ID
     */
    public List<RetrievedDocument> findSimilarDocuments(String documentId, int maxResults) {
        try {
            // This would require finding the document first, then searching
            // For now, we'll need to implement this based on your specific needs
            logger.warn("findSimilarDocuments not yet implemented for document ID: {}", documentId);
            return List.of();
        } catch (Exception e) {
            logger.error("Error finding similar documents for ID: {}", documentId, e);
            throw new RuntimeException("Failed to find similar documents", e);
        }
    }
    
    /**
     * Convert LangChain4j search result to our domain model
     */
    private RetrievedDocument convertToRetrievedDocument(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();
        
        // Extract ID from metadata
        String id = metadata.getString("id");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        
        // Convert metadata to Map
        Map<String, Object> metadataMap = new HashMap<>();
        metadata.asMap().forEach(metadataMap::put);
        
        return new RetrievedDocument(
                id,
                segment.text(),
                match.score(),
                metadataMap
        );
    }
    
    /**
     * Build metadata filter from search filters
     */
    private Metadata buildMetadataFilter(List<SearchFilter> filters) {
        Metadata metadata = new Metadata();
        
        for (SearchFilter filter : filters) {
            switch (filter.operator()) {
                case EQUALS -> metadata.put(filter.field(), filter.value());
                case CONTAINS -> {
                    // For contains, we'll need to implement custom logic
                    // This is a simplified approach
                    metadata.put(filter.field(), filter.value());
                }
                case GREATER_THAN, LESS_THAN -> {
                    // For numeric comparisons, convert value
                    try {
                        Double numValue = Double.parseDouble(filter.value());
                        metadata.put(filter.field(), numValue);
                    } catch (NumberFormatException e) {
                        logger.warn("Cannot convert '{}' to number for comparison", filter.value());
                        metadata.put(filter.field(), filter.value());
                    }
                }
            }
        }
        
        return metadata;
    }
    
    /**
     * Get embedding store statistics
     */
    public VectorStoreStats getStats() {
        try {
            // Note: OpenSearchEmbeddingStore might not provide direct count methods
            // This is a placeholder implementation
            return new VectorStoreStats(
                    "OpenSearch",
                    -1, // Count not available
                    1536, // Titan embedding dimension
                    "cosine" // Default similarity metric
            );
        } catch (Exception e) {
            logger.error("Error getting vector store stats", e);
            return new VectorStoreStats("OpenSearch", -1, 1536, "cosine");
        }
    }
    
    /**
     * Document for indexing record
     */
    public record DocumentForIndexing(
            String content,
            Map<String, Object> metadata
    ) {}
    
    /**
     * Vector store statistics record
     */
    public record VectorStoreStats(
            String storeType,
            long documentCount,
            int dimension,
            String similarityMetric
    ) {}
}

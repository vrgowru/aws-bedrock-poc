package com.example.ragapi.service;

import com.example.ragapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final VectorSearchService vectorSearchService;
    private final TextChunkingService textChunkingService;
    
    @Value("${rag.processing.chunk-size:1000}")
    private int defaultChunkSize;
    
    @Value("${rag.processing.chunk-overlap:200}")
    private int defaultChunkOverlap;
    
    @Value("${rag.processing.max-chunks-per-document:50}")
    private int maxChunksPerDocument;
    
    public DocumentService(VectorSearchService vectorSearchService, TextChunkingService textChunkingService) {
        this.vectorSearchService = vectorSearchService;
        this.textChunkingService = textChunkingService;
    }
    
    /**
     * Index a single document with automatic chunking
     */
    public String indexDocument(String content, Map<String, Object> metadata) {
        try {
            String documentId = UUID.randomUUID().toString();
            logger.info("Indexing document with ID: {} and content length: {}", documentId, content.length());
            
            // Add document ID to metadata
            Map<String, Object> enrichedMetadata = enrichMetadata(metadata, documentId);
            
            // Check if document needs chunking
            if (content.length() <= defaultChunkSize) {
                // Small document - index as single chunk
                vectorSearchService.addDocument(content, enrichedMetadata);
                logger.debug("Document {} indexed as single chunk", documentId);
            } else {
                // Large document - chunk and index
                List<String> chunks = textChunkingService.chunkText(content, defaultChunkSize, defaultChunkOverlap);
                
                if (chunks.size() > maxChunksPerDocument) {
                    logger.warn("Document {} has {} chunks, truncating to {}", 
                               documentId, chunks.size(), maxChunksPerDocument);
                    chunks = chunks.subList(0, maxChunksPerDocument);
                }
                
                // Index each chunk with metadata indicating chunk info
                List<VectorSearchService.DocumentForIndexing> documentsToIndex = chunks.stream()
                        .map(chunk -> {
                            Map<String, Object> chunkMetadata = enrichMetadata(enrichedMetadata, documentId);
                            chunkMetadata.put("chunk_index", chunks.indexOf(chunk));
                            chunkMetadata.put("total_chunks", chunks.size());
                            chunkMetadata.put("is_chunk", true);
                            return new VectorSearchService.DocumentForIndexing(chunk, chunkMetadata);
                        })
                        .toList();
                
                vectorSearchService.addDocuments(documentsToIndex);
                logger.info("Document {} indexed as {} chunks", documentId, chunks.size());
            }
            
            return documentId;
            
        } catch (Exception e) {
            logger.error("Error indexing document", e);
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * Index multiple documents
     */
    public List<String> indexDocuments(List<DocumentDto> documents) {
        try {
            logger.info("Indexing {} documents", documents.size());
            
            return documents.stream()
                    .map(doc -> {
                        try {
                            return indexDocument(doc.content(), doc.metadata());
                        } catch (Exception e) {
                            logger.error("Failed to index document: {}", doc.id(), e);
                            throw new RuntimeException("Failed to index document: " + doc.id(), e);
                        }
                    })
                    .toList();
            
        } catch (Exception e) {
            logger.error("Error indexing multiple documents", e);
            throw new RuntimeException("Failed to index documents", e);
        }
    }
    
    /**
     * Delete a document and all its chunks
     */
    public void deleteDocument(String documentId) {
        try {
            logger.info("Deleting document: {}", documentId);
            
            // For now, we'll delete by document ID
            // In a more sophisticated implementation, we might need to:
            // 1. Find all chunks with the same document ID
            // 2. Delete them individually
            vectorSearchService.deleteDocuments(List.of(documentId));
            
            logger.info("Successfully deleted document: {}", documentId);
            
        } catch (Exception e) {
            logger.error("Error deleting document: {}", documentId, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
    
    /**
     * Delete multiple documents
     */
    public void deleteDocuments(List<String> documentIds) {
        try {
            logger.info("Deleting {} documents", documentIds.size());
            vectorSearchService.deleteDocuments(documentIds);
            logger.info("Successfully deleted {} documents", documentIds.size());
        } catch (Exception e) {
            logger.error("Error deleting documents", e);
            throw new RuntimeException("Failed to delete documents", e);
        }
    }
    
    /**
     * Search documents using text query
     */
    public List<RetrievedDocument> searchDocuments(
            String query, 
            int maxResults,
            List<SearchFilter> filters,
            double threshold) {
        
        try {
            logger.debug("Searching documents with query: {}", 
                        query.substring(0, Math.min(50, query.length())));
            
            return vectorSearchService.searchSimilarDocuments(query, maxResults, filters, threshold);
            
        } catch (Exception e) {
            logger.error("Error searching documents", e);
            throw new RuntimeException("Failed to search documents", e);
        }
    }
    
    /**
     * Get document statistics
     */
    public DocumentStats getDocumentStats() {
        try {
            VectorSearchService.VectorStoreStats storeStats = vectorSearchService.getStats();
            
            return new DocumentStats(
                    storeStats.documentCount(),
                    defaultChunkSize,
                    defaultChunkOverlap,
                    maxChunksPerDocument,
                    storeStats.storeType()
            );
            
        } catch (Exception e) {
            logger.error("Error getting document stats", e);
            return new DocumentStats(-1, defaultChunkSize, defaultChunkOverlap, maxChunksPerDocument, "unknown");
        }
    }
    
    /**
     * Enrich metadata with standard fields
     */
    private Map<String, Object> enrichMetadata(Map<String, Object> originalMetadata, String documentId) {
        Map<String, Object> enriched = originalMetadata != null ? 
                new java.util.HashMap<>(originalMetadata) : new java.util.HashMap<>();
        
        enriched.put("document_id", documentId);
        enriched.put("indexed_at", System.currentTimeMillis());
        enriched.put("indexer_version", "1.0.0");
        
        // Add default values if not present
        enriched.putIfAbsent("title", "Untitled Document");
        enriched.putIfAbsent("source", "unknown");
        enriched.putIfAbsent("content_type", "text");
        
        return enriched;
    }
    
    /**
     * Document statistics record
     */
    public record DocumentStats(
            long totalDocuments,
            int chunkSize,
            int chunkOverlap,
            int maxChunksPerDocument,
            String storeType
    ) {}
}

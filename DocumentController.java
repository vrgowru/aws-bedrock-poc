package com.example.ragapi.controller;

import com.example.ragapi.model.*;
import com.example.ragapi.service.DocumentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    /**
     * Index a single document
     */
    @PostMapping("/index")
    public ResponseEntity<IndexDocumentResponse> indexDocument(@Valid @RequestBody IndexDocumentRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Received request to index document with content length: {}", 
                       request.content().length());
            
            String documentId = documentService.indexDocument(request.content(), request.metadata());
            
            long processingTime = System.currentTimeMillis() - startTime;
            IndexDocumentResponse response = IndexDocumentResponse.success(documentId, processingTime);
            
            logger.info("Successfully indexed document {} in {}ms", documentId, processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error indexing document", e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            IndexDocumentResponse response = IndexDocumentResponse.error(
                "Failed to index document: " + e.getMessage(),
                processingTime
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Index multiple documents in batch
     */
    @PostMapping("/index/batch")
    public ResponseEntity<BatchIndexResponse> indexDocumentsBatch(@Valid @RequestBody BatchIndexRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Received request to index {} documents", request.documents().size());
            
            List<String> documentIds = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int successful = 0;
            
            for (DocumentDto doc : request.documents()) {
                try {
                    String documentId = documentService.indexDocument(doc.content(), doc.metadata());
                    documentIds.add(documentId);
                    successful++;
                    
                    logger.debug("Successfully indexed document: {}", documentId);
                    
                } catch (Exception e) {
                    String error = String.format("Failed to index document %s: %s", 
                                                doc.id() != null ? doc.id() : "unknown", 
                                                e.getMessage());
                    errors.add(error);
                    logger.warn("Failed to index document: {}", error);
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            BatchIndexResponse response;
            if (errors.isEmpty()) {
                response = BatchIndexResponse.success(request.documents().size(), documentIds, processingTime);
                logger.info("Successfully indexed all {} documents in {}ms", successful, processingTime);
            } else {
                response = BatchIndexResponse.partial(request.documents().size(), successful, 
                                                    documentIds, errors, processingTime);
                logger.warn("Indexed {}/{} documents successfully in {}ms with {} errors", 
                           successful, request.documents().size(), processingTime, errors.size());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during batch indexing", e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            BatchIndexResponse response = BatchIndexResponse.partial(
                request.documents().size(), 0, List.of(),
                List.of("Batch indexing failed: " + e.getMessage()),
                processingTime
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Delete a single document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<DeleteDocumentsResponse> deleteDocument(@PathVariable String documentId) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Received request to delete document: {}", documentId);
            
            documentService.deleteDocument(documentId);
            
            long processingTime = System.currentTimeMillis() - startTime;
            DeleteDocumentsResponse response = DeleteDocumentsResponse.success(1, processingTime);
            
            logger.info("Successfully deleted document {} in {}ms", documentId, processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting document: {}", documentId, e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            DeleteDocumentsResponse response = DeleteDocumentsResponse.partial(
                1, 0, 
                List.of("Failed to delete document " + documentId + ": " + e.getMessage()),
                processingTime
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Delete multiple documents
     */
    @DeleteMapping("/batch")
    public ResponseEntity<DeleteDocumentsResponse> deleteDocuments(@Valid @RequestBody DeleteDocumentsRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Received request to delete {} documents", request.documentIds().size());
            
            List<String> errors = new ArrayList<>();
            int successful = 0;
            
            for (String documentId : request.documentIds()) {
                try {
                    documentService.deleteDocument(documentId);
                    successful++;
                    logger.debug("Successfully deleted document: {}", documentId);
                    
                } catch (Exception e) {
                    String error = String.format("Failed to delete document %s: %s", 
                                                documentId, e.getMessage());
                    errors.add(error);
                    logger.warn("Failed to delete document: {}", error);
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            DeleteDocumentsResponse response;
            if (errors.isEmpty()) {
                response = DeleteDocumentsResponse.success(request.documentIds().size(), processingTime);
                logger.info("Successfully deleted all {} documents in {}ms", successful, processingTime);
            } else {
                response = DeleteDocumentsResponse.partial(request.documentIds().size(), successful, 
                                                         errors, processingTime);
                logger.warn("Deleted {}/{} documents successfully in {}ms with {} errors", 
                           successful, request.documentIds().size(), processingTime, errors.size());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during batch deletion", e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            DeleteDocumentsResponse response = DeleteDocumentsResponse.partial(
                request.documentIds().size(), 0,
                List.of("Batch deletion failed: " + e.getMessage()),
                processingTime
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Search documents (convenience endpoint)
     */
    @PostMapping("/search")
    public ResponseEntity<VectorSearchResponse> searchDocuments(@Valid @RequestBody QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Received document search request for: {}", 
                        request.question().substring(0, Math.min(50, request.question().length())));
            
            // Use the document service to search
            var documents = documentService.searchDocuments(
                request.question(),
                request.maxResults(),
                request.filters(),
                request.threshold()
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            double maxScore = documents.stream().mapToDouble(RetrievedDocument::score).max().orElse(0.0);
            double minScore = documents.stream().mapToDouble(RetrievedDocument::score).min().orElse(0.0);
            
            VectorSearchResponse response = new VectorSearchResponse(
                documents,
                documents.size(),
                maxScore,
                minScore,
                searchTime
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new VectorSearchResponse(List.of(), 0, 0.0, 0.0, 
                     System.currentTimeMillis() - startTime));
        }
    }
    
    /**
     * Get document by ID (if supported by vector store)
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable String documentId) {
        try {
            logger.debug("Received request to get document: {}", documentId);
            
            // This would require additional implementation in the vector store
            // For now, return not implemented
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                    "message", "Get document by ID not yet implemented",
                    "documentId", documentId,
                    "suggestion", "Use search endpoint to find documents"
                ));
            
        } catch (Exception e) {
            logger.error("Error getting document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

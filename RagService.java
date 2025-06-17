package com.example.ragapi.service;

import com.example.ragapi.model.QueryRequest;
import com.example.ragapi.model.QueryResponse;
import com.example.ragapi.model.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final VectorSearchService vectorSearchService;
    private final ClaudeService claudeService;
    
    public RagService(
            VectorSearchService vectorSearchService,
            ClaudeService claudeService) {
        this.vectorSearchService = vectorSearchService;
        this.claudeService = claudeService;
    }
    
    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Processing RAG query: {}", request.question());
            
            // Step 1: Search for similar documents using Spring AI
            logger.debug("Step 1: Searching for similar documents");
            List<RetrievedDocument> retrievedDocs = vectorSearchService.searchSimilarDocuments(
                    request.question(),
                    request.maxResults(),
                    request.filters(),
                    request.threshold()
            );
            
            if (retrievedDocs.isEmpty()) {
                logger.warn("No documents found for query");
                return new QueryResponse(
                        "I couldn't find any relevant documents to answer your question. Please try rephrasing your question or check if the relevant documents are available in the knowledge base.",
                        List.of(),
                        0.0,
                        System.currentTimeMillis() - startTime
                );
            }
            
            // Step 2: Generate answer using Claude
            logger.debug("Step 2: Generating answer using Claude with {} documents", retrievedDocs.size());
            String answer = claudeService.generateAnswer(request.question(), retrievedDocs);
            
            // Step 3: Calculate confidence score
            double confidence = claudeService.calculateConfidence(retrievedDocs);
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("RAG query processed successfully in {}ms", processingTime);
            
            return new QueryResponse(
                    answer,
                    retrievedDocs,
                    confidence,
                    processingTime
            );
            
        } catch (Exception e) {
            logger.error("Error processing RAG query", e);
            long processingTime = System.currentTimeMillis() - startTime;
            
            return new QueryResponse(
                    "I encountered an error while processing your question. Please try again later.",
                    List.of(),
                    0.0,
                    processingTime
            );
        }
    }
}
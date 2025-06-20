package com.example.ragapi.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final BedrockTitanEmbeddingModel embeddingModel;
    private final ExecutorService executorService;
    
    public EmbeddingService(BedrockTitanEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    /**
     * Generate embedding for a single text using Titan model
     */
    public List<Float> generateEmbedding(String text) {
        try {
            logger.debug("Generating embedding for text of length: {}", text.length());
            
            // Validate input
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Text cannot be null or empty");
            }
            
            // Truncate text if too long (Titan has limits)
            String processedText = preprocessText(text);
            
            // Generate embedding using LangChain4j
            Response<Embedding> response = embeddingModel.embed(processedText);
            
            if (response.content() == null) {
                throw new RuntimeException("Failed to generate embedding - null response");
            }
            
            List<Float> embedding = response.content().vector();
            logger.debug("Generated embedding with {} dimensions", embedding.size());
            
            return embedding;
            
        } catch (Exception e) {
            logger.error("Error generating embedding for text: {}", text.substring(0, Math.min(100, text.length())), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    /**
     * Generate embeddings for multiple texts in parallel
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        try {
            logger.debug("Generating embeddings for {} texts", texts.size());
            
            if (texts == null || texts.isEmpty()) {
                throw new IllegalArgumentException("Texts list cannot be null or empty");
            }
            
            // Process in parallel using virtual threads
            List<CompletableFuture<List<Float>>> futures = texts.stream()
                    .map(text -> CompletableFuture.supplyAsync(() -> generateEmbedding(text), executorService))
                    .toList();
            
            // Wait for all embeddings to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            
            return allOf.thenApply(v -> 
                    futures.stream()
                            .map(CompletableFuture::join)
                            .toList()
            ).join();
            
        } catch (Exception e) {
            logger.error("Error generating embeddings for {} texts", texts.size(), e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }
    
    /**
     * Generate embedding asynchronously
     */
    public CompletableFuture<List<Float>> generateEmbeddingAsync(String text) {
        return CompletableFuture.supplyAsync(() -> generateEmbedding(text), executorService);
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    public double calculateSimilarity(List<Float> embedding1, List<Float> embedding2) {
        if (embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            float val1 = embedding1.get(i);
            float val2 = embedding2.get(i);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Get embedding model information
     */
    public EmbeddingModelInfo getModelInfo() {
        return new EmbeddingModelInfo(
                "amazon.titan-embed-text-v1",
                1536,
                "AWS Bedrock Titan Text Embedding Model",
                8192 // Max input tokens
        );
    }
    
    /**
     * Preprocess text before embedding generation
     */
    private String preprocessText(String text) {
        // Titan has a limit of approximately 8192 tokens
        // Rough estimate: 1 token ≈ 4 characters
        final int MAX_CHARS = 30000; // Conservative estimate
        
        if (text.length() > MAX_CHARS) {
            logger.warn("Text too long ({}), truncating to {} characters", text.length(), MAX_CHARS);
            return text.substring(0, MAX_CHARS) + "...";
        }
        
        // Clean up the text
        return text.trim()
                   .replaceAll("\\s+", " ") // Normalize whitespace
                   .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", ""); // Remove control characters
    }
    
    /**
     * Validate embedding dimension
     */
    public boolean isValidEmbedding(List<Float> embedding) {
        return embedding != null && 
               embedding.size() == 1536 && 
               embedding.stream().allMatch(f -> f != null && Float.isFinite(f));
    }
    
    /**
     * Record for embedding model information
     */
    public record EmbeddingModelInfo(
            String modelId,
            int dimension,
            String description,
            int maxInputTokens
    ) {}
}

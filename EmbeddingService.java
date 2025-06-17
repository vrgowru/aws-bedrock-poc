package com.example.ragapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final BedrockTitanEmbeddingModel embeddingModel;
    
    public EmbeddingService(BedrockTitanEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    public List<Double> generateEmbedding(String text) {
        try {
            logger.debug("Generating embedding for text of length: {}", text.length());
            
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            if (response.getResults().isEmpty()) {
                throw new RuntimeException("No embedding generated");
            }
            
            List<Double> embedding = response.getResults().get(0).getOutput();
            logger.debug("Generated embedding with {} dimensions", embedding.size());
            
            return embedding;
            
        } catch (Exception e) {
            logger.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        try {
            logger.debug("Generating embeddings for {} texts", texts.size());
            
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            return response.getResults().stream()
                    .map(result -> result.getOutput())
                    .toList();
            
        } catch (Exception e) {
            logger.error("Error generating embeddings", e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }
}
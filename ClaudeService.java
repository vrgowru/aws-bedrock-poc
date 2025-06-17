package com.example.ragapi.service;

import com.example.ragapi.model.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class ClaudeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);
    
    private final BedrockAnthropicChatModel chatModel;
    
    private static final String RAG_PROMPT_TEMPLATE = """
        You are a helpful AI assistant. Answer the user's question based on the provided context documents.
        If the context doesn't contain enough information to answer the question, say so clearly.
        Be concise but comprehensive in your response.
        
        Context Documents:
        {context}
        
        User Question: {question}
        
        Please provide a clear, accurate answer based on the context documents above:
        """;
    
    public ClaudeService(BedrockAnthropicChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    public String generateAnswer(String question, List<RetrievedDocument> retrievedDocs) {
        try {
            logger.debug("Generating answer for question with {} retrieved documents", 
                    retrievedDocs.size());
            
            String context = buildContext(retrievedDocs);
            
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "context", context,
                    "question", question
            ));
            
            logger.debug("Sending request to Claude model");
            
            ChatResponse response = chatModel.call(prompt);
            
            String answer = response.getResult().getOutput().getContent();
            logger.debug("Generated answer of length: {}", answer.length());
            
            return answer;
            
        } catch (Exception e) {
            logger.error("Error generating answer with Claude", e);
            throw new RuntimeException("Failed to generate answer", e);
        }
    }
    
    private String buildContext(List<RetrievedDocument> retrievedDocs) {
        StringBuilder context = new StringBuilder();
        
        IntStream.range(0, retrievedDocs.size())
                .forEach(i -> {
                    RetrievedDocument doc = retrievedDocs.get(i);
                    context.append("Document ").append(i + 1).append(":\n");
                    
                    // Add metadata if available
                    if (doc.metadata() != null) {
                        String title = (String) doc.metadata().get("title");
                        String source = (String) doc.metadata().get("source");
                        
                        if (title != null) {
                            context.append("Title: ").append(title).append("\n");
                        }
                        if (source != null) {
                            context.append("Source: ").append(source).append("\n");
                        }
                    }
                    
                    context.append("Content: ").append(doc.content()).append("\n");
                    context.append("Relevance Score: ").append(String.format("%.3f", doc.score())).append("\n\n");
                });
        
        return context.toString();
    }
    
    public double calculateConfidence(List<RetrievedDocument> retrievedDocs) {
        if (retrievedDocs.isEmpty()) {
            return 0.0;
        }
        
        // Calculate confidence based on the average score of top documents
        double avgScore = retrievedDocs.stream()
                .limit(3) // Consider top 3 documents
                .mapToDouble(RetrievedDocument::score)
                .average()
                .orElse(0.0);
        
        // Normalize to 0-1 range (assuming cosine similarity scores)
        return Math.min(Math.max(avgScore, 0.0), 1.0);
    }
}
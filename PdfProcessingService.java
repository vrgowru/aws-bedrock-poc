package com.example.ragpoc.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import com.example.ragpoc.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PdfProcessingService.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${rag.chunk-size}")
    private int chunkSize;

    @Value("${rag.chunk-overlap}")
    private int chunkOverlap;

    public List<DocumentChunk> processPdfFile(MultipartFile file) {
        try {
            logger.info("Processing PDF file: {}", file.getOriginalFilename());
            
            // Parse PDF document
            Document document = parseDocument(file);
            
            // Split document into chunks
            List<TextSegment> segments = splitDocument(document);
            
            // Create embeddings for each segment
            List<DocumentChunk> chunks = createEmbeddings(segments, file.getOriginalFilename());
            
            logger.info("Successfully processed {} chunks from PDF", chunks.size());
            return chunks;
            
        } catch (Exception e) {
            logger.error("Error processing PDF file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PDF file", e);
        }
    }

    private Document parseDocument(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
            return parser.parse(inputStream);
        }
    }

    private List<TextSegment> splitDocument(Document document) {
        return DocumentSplitters.recursive(chunkSize, chunkOverlap)
                .split(document);
    }

    private List<DocumentChunk> createEmbeddings(List<TextSegment> segments, String filename) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String content = segment.text();
            
            if (content.trim().isEmpty()) {
                continue;
            }
            
            try {
                // Generate embedding for the segment
                Embedding embedding = embeddingModel.embed(content).content();
                
                // Convert to List<Float>
                List<Float> embeddingVector = new ArrayList<>();
                for (int j = 0; j < embedding.dimension(); j++) {
                    embeddingVector.add(embedding.vector()[j]);
                }
                
                // Create document chunk
                DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID().toString(),
                    content,
                    embeddingVector,
                    filename,
                    i
                );
                
                chunks.add(chunk);
                logger.debug("Created embedding for chunk {} with dimension {}", i, embedding.dimension());
                
            } catch (Exception e) {
                logger.error("Failed to create embedding for chunk {}: {}", i, e.getMessage());
            }
        }
        
        return chunks;
    }
}

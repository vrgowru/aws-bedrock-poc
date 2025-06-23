
package com.example.ragpoc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DocumentChunk {
    private String id;
    private String content;
    private List<Float> embedding;
    private String sourceFile;
    private int chunkIndex;
    private long timestamp;

    public DocumentChunk() {}

    public DocumentChunk(String id, String content, List<Float> embedding, String sourceFile, int chunkIndex) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
        this.sourceFile = sourceFile;
        this.chunkIndex = chunkIndex;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

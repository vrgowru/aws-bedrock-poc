package com.benefitsassist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA Entity for Benefits Assist Inference History
 */
@Entity
@Table(name = "benefits_assist_inference_history")
public class BenefitsAssistInferenceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "client_name", length = 255, nullable = false)
    private String clientName;

    @Column(name = "client_session_id", length = 255)
    private String clientSessionId;

    @NotBlank
    @Column(name = "tracking_id", length = 255, nullable = false)
    private String trackingId;

    @NotNull
    @Column(name = "submit_date_time", nullable = false)
    private OffsetDateTime submitDateTime;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @NotBlank
    @Column(name = "conversation_id", length = 255, nullable = false)
    private String conversationId;

    @NotBlank
    @Column(name = "message_id", length = 255, nullable = false)
    private String messageId;

    @NotNull
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @NotBlank
    @Lob
    @Column(name = "user_query", nullable = false)
    private String userQuery;

    @NotBlank
    @Lob
    @Column(name = "response_text", nullable = false)
    private String responseText;

    @NotBlank
    @Column(name = "coverage_package_codes", length = 255, nullable = false)
    private String coveragePackageCodes;

    @Column(name = "group_anniversary_date")
    private LocalDate groupAnniversaryDate;

    @Column(name = "state_code", length = 20)
    private String stateCode;

    @Column(name = "conversation_model_name", length = 255)
    private String conversationModelName;

    @Column(name = "conversation_model_version", length = 50)
    private String conversationModelVersion;

    @Column(name = "embedding_model_name", length = 255)
    private String embeddingModelName;

    @Column(name = "embedding_model_version", length = 50)
    private String embeddingModelVersion;

    @Column(name = "inference_confidence_score", length = 50)
    private String inferenceConfidenceScore;

    @Column(name = "conversation_intent", length = 255)
    private String conversationIntent;

    @Column(name = "prompt_tokens", length = 255)
    private String promptTokens;

    @Column(name = "completion_tokens", length = 255)
    private String completionTokens;

    @Column(name = "total_tokens", length = 255)
    private String totalTokens;

    @Column(name = "rag_chunks_retrieved", length = 255)
    private String ragChunksRetrieved;

    @Column(name = "rag_retrieval_score", length = 255)
    private String ragRetrievalScore;

    // Constructors
    public BenefitsAssistInferenceHistory() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public OffsetDateTime getSubmitDateTime() {
        return submitDateTime;
    }

    public void setSubmitDateTime(OffsetDateTime submitDateTime) {
        this.submitDateTime = submitDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getCoveragePackageCodes() {
        return coveragePackageCodes;
    }

    public void setCoveragePackageCodes(String coveragePackageCodes) {
        this.coveragePackageCodes = coveragePackageCodes;
    }

    public LocalDate getGroupAnniversaryDate() {
        return groupAnniversaryDate;
    }

    public void setGroupAnniversaryDate(LocalDate groupAnniversaryDate) {
        this.groupAnniversaryDate = groupAnniversaryDate;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getConversationModelName() {
        return conversationModelName;
    }

    public void setConversationModelName(String conversationModelName) {
        this.conversationModelName = conversationModelName;
    }

    public String getConversationModelVersion() {
        return conversationModelVersion;
    }

    public void setConversationModelVersion(String conversationModelVersion) {
        this.conversationModelVersion = conversationModelVersion;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public String getEmbeddingModelVersion() {
        return embeddingModelVersion;
    }

    public void setEmbeddingModelVersion(String embeddingModelVersion) {
        this.embeddingModelVersion = embeddingModelVersion;
    }

    public String getInferenceConfidenceScore() {
        return inferenceConfidenceScore;
    }

    public void setInferenceConfidenceScore(String inferenceConfidenceScore) {
        this.inferenceConfidenceScore = inferenceConfidenceScore;
    }

    public String getConversationIntent() {
        return conversationIntent;
    }

    public void setConversationIntent(String conversationIntent) {
        this.conversationIntent = conversationIntent;
    }

    public String getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(String promptTokens) {
        this.promptTokens = promptTokens;
    }

    public String getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(String completionTokens) {
        this.completionTokens = completionTokens;
    }

    public String getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(String totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getRagChunksRetrieved() {
        return ragChunksRetrieved;
    }

    public void setRagChunksRetrieved(String ragChunksRetrieved) {
        this.ragChunksRetrieved = ragChunksRetrieved;
    }

    public String getRagRetrievalScore() {
        return ragRetrievalScore;
    }

    public void setRagRetrievalScore(String ragRetrievalScore) {
        this.ragRetrievalScore = ragRetrievalScore;
    }

    @Override
    public String toString() {
        return "BenefitsAssistInferenceHistory{" +
                "id=" + id +
                ", clientName='" + clientName + '\'' +
                ", trackingId='" + trackingId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", requestDate=" + requestDate +
                ", conversationModelName='" + conversationModelName + '\'' +
                ", totalTokens='" + totalTokens + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BenefitsAssistInferenceHistory)) return false;
        BenefitsAssistInferenceHistory that = (BenefitsAssistInferenceHistory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

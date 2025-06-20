package com.example.ragapi.config;

import dev.langchain4j.model.bedrock.BedrockAnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Duration;

@Configuration
public class LangChain4jConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${opensearch.endpoint}")
    private String openSearchEndpoint;

    @Value("${opensearch.index.name:documents}")
    private String indexName;

    @Value("${bedrock.embedding.model:amazon.titan-embed-text-v1}")
    private String embeddingModelId;

    @Value("${bedrock.claude.model:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String claudeModelId;

    @Value("${opensearch.username:}")
    private String openSearchUsername;

    @Value("${opensearch.password:}")
    private String openSearchPassword;

    /**
     * Configure AWS Bedrock Titan Embedding Model
     */
    @Bean
    public BedrockTitanEmbeddingModel titanEmbeddingModel() {
        logger.info("Configuring Bedrock Titan Embedding Model: {} in region: {}", 
                   embeddingModelId, awsRegion);
        
        return BedrockTitanEmbeddingModel.builder()
                .modelId(embeddingModelId)
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .timeout(Duration.ofMinutes(2))
                .maxRetries(3)
                .build();
    }

    /**
     * Configure AWS Bedrock Anthropic Chat Model (Claude)
     */
    @Bean
    public BedrockAnthropicChatModel anthropicChatModel() {
        logger.info("Configuring Bedrock Anthropic Chat Model: {} in region: {}", 
                   claudeModelId, awsRegion);
        
        return BedrockAnthropicChatModel.builder()
                .modelId(claudeModelId)
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .temperature(0.7)
                .maxTokens(4000)
                .timeout(Duration.ofMinutes(5))
                .maxRetries(3)
                .build();
    }

    /**
     * Configure OpenSearch Client
     */
    @Bean
    public OpenSearchClient openSearchClient() {
        logger.info("Configuring OpenSearch client for endpoint: {}", openSearchEndpoint);
        
        var transportBuilder = ApacheHttpClient5TransportBuilder
                .builder(URI.create(openSearchEndpoint))
                .setMapper(new JacksonJsonpMapper());

        // Add authentication if credentials are provided
        if (!openSearchUsername.isEmpty() && !openSearchPassword.isEmpty()) {
            logger.info("Configuring OpenSearch with authentication for user: {}", openSearchUsername);
            transportBuilder.setCredentials(openSearchUsername, openSearchPassword);
        }

        var transport = transportBuilder.build();
        return new OpenSearchClient(transport);
    }

    /**
     * Configure OpenSearch Embedding Store with Titan embeddings
     */
    @Bean
    public OpenSearchEmbeddingStore openSearchEmbeddingStore(
            BedrockTitanEmbeddingModel embeddingModel) {
        
        logger.info("Configuring OpenSearch Embedding Store with index: {}", indexName);
        
        var storeBuilder = OpenSearchEmbeddingStore.builder()
                .serverUrl(openSearchEndpoint)
                .indexName(indexName)
                .dimension(1536); // Titan embedding dimension

        // Add authentication if credentials are provided
        if (!openSearchUsername.isEmpty() && !openSearchPassword.isEmpty()) {
            storeBuilder.userName(openSearchUsername)
                      .password(openSearchPassword);
        }

        return storeBuilder.build();
    }

    /**
     * Configuration properties for embedding model settings
     */
    @Bean
    public EmbeddingModelConfig embeddingModelConfig() {
        return new EmbeddingModelConfig(
                embeddingModelId,
                1536,  // Titan embedding dimension
                0.7,   // Default similarity threshold
                5      // Default max results
        );
    }

    /**
     * Configuration properties record
     */
    public record EmbeddingModelConfig(
            String modelId,
            int dimension,
            double defaultThreshold,
            int defaultMaxResults
    ) {}
}

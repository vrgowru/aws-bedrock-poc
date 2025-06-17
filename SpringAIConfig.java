package com.example.ragapi.config;

import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;
import org.springframework.ai.bedrock.anthropic.api.AnthropicChatBedrockApi;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.opensearch.OpenSearchVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.bedrock.aws.region:us-east-1}")
    private String awsRegion;

    @Value("${spring.ai.bedrock.titan.embedding.model:amazon.titan-embed-text-v1}")
    private String titanEmbeddingModel;

    @Value("${spring.ai.bedrock.anthropic.chat.model:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String anthropicChatModel;

    @Value("${spring.ai.opensearch.url}")
    private String openSearchUrl;

    @Value("${spring.ai.opensearch.index-name:documents}")
    private String indexName;

    @Bean
    public BedrockTitanEmbeddingModel bedrockTitanEmbeddingModel() {
        var titanApi = TitanEmbeddingBedrockApi.builder()
                .withCredentialsProvider(DefaultCredentialsProvider.create())
                .withRegion(Region.of(awsRegion))
                .withModel(titanEmbeddingModel)
                .build();

        return new BedrockTitanEmbeddingModel(titanApi);
    }

    @Bean
    public BedrockAnthropicChatModel bedrockAnthropicChatModel() {
        var anthropicApi = AnthropicChatBedrockApi.builder()
                .withCredentialsProvider(DefaultCredentialsProvider.create())
                .withRegion(Region.of(awsRegion))
                .withModel(anthropicChatModel)
                .build();

        return new BedrockAnthropicChatModel(anthropicApi);
    }

    @Bean
    public OpenSearchVectorStore openSearchVectorStore(BedrockTitanEmbeddingModel embeddingModel) {
        return OpenSearchVectorStore.builder()
                .withUrl(openSearchUrl)
                .withIndexName(indexName)
                .withEmbeddingModel(embeddingModel)
                .build();
    }
}
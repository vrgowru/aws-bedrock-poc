package com.example.ragpoc.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.url}")
    private String opensearchUrl;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            URI uri = URI.create(opensearchUrl);
            
            // Create credentials provider
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
            );

            // Create the low-level client
            RestClientBuilder builder = RestClient.builder(
                new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())
            ).setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );

            RestClient restClient = builder.build();

            // Create the transport with a Jackson mapper
            RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
            );

            // Create the API client
            return new OpenSearchClient(transport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpenSearch client", e);
        }
    }
}

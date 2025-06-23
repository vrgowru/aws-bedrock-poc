package com.example.ragpoc.service;

import com.example.ragpoc.model.DocumentChunk;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.IntegerNumberProperty;
import org.opensearch.client.opensearch._types.mapping.LongNumberProperty;
import org.opensearch.client.opensearch._types.mapping.DenseVectorProperty;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenSearchService {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);

    @Autowired
    private OpenSearchClient client;

    @Value("${opensearch.index-name}")
    private String indexName;

    @PostConstruct
    public void initializeIndex() {
        try {
            if (!indexExists()) {
                createIndex();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize OpenSearch index", e);
        }
    }

    private boolean indexExists() throws IOException {
        ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
        return client.indices().exists(request).value();
    }

    private void createIndex() throws IOException {
        logger.info("Creating OpenSearch index: {}", indexName);
        
        Map<String, Property> properties = new HashMap<>();
        properties.put("content", Property.of(p -> p.text(TextProperty.of(t -> t))));
        properties.put("sourceFile", Property.of(p -> p.text(TextProperty.of(t -> t))));
        properties.put("chunkIndex", Property.of(p -> p.integer(IntegerNumberProperty.of(i -> i))));
        properties.put("timestamp", Property.of(p -> p.long_(LongNumberProperty.of(l -> l))));
        properties.put("embedding", Property.of(p -> p.denseVector(DenseVectorProperty.of(d -> d.dims(768)))));

        CreateIndexRequest request = CreateIndexRequest.of(c -> c
            .index(indexName)
            .mappings(m -> m.properties(properties))
            .settings(s -> s
                .index(i -> i
                    .knn(true)
                    .numberOfShards("1")
                    .numberOfReplicas("0")
                )
            )
        );

        client.indices().create(request);
        logger.info("Successfully created OpenSearch index: {}", indexName);
    }

    public void indexDocumentChunk(DocumentChunk chunk) {
        try {
            IndexRequest<DocumentChunk> request = IndexRequest.of(i -> i
                .index(indexName)
                .id(chunk.getId())
                .document(chunk)
            );

            client.index(request);
            logger.debug("Indexed document chunk: {}", chunk.getId());
        } catch (Exception e) {
            logger.error("Failed to index document chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to index document chunk", e);
        }
    }

    public void indexDocumentChunks(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            indexDocumentChunk(chunk);
        }
        logger.info("Successfully indexed {} document chunks", chunks.size());
    }

    public List<DocumentChunk> searchSimilarChunks(List<Float> queryEmbedding, int size) {
        try {
            KnnQuery knnQuery = KnnQuery.of(k -> k
                .field("embedding")
                .vector(queryEmbedding.toArray(new Float[0]))
                .k(size)
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(Query.of(q -> q.knn(knnQuery)))
                .size(size)
            );

            SearchResponse<DocumentChunk> response = client.search(request, DocumentChunk.class);
            
            List<DocumentChunk> results = new ArrayList<>();
            for (Hit<DocumentChunk> hit : response.hits().hits()) {
                DocumentChunk chunk = hit.source();
                if (chunk != null) {
                    results.add(chunk);
                }
            }
            
            logger.debug("Found {} similar chunks", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to search similar chunks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search similar chunks", e);
        }
    }
}

# RAG API with Spring AI - Deployment Guide

## Prerequisites

1. **AWS CLI** configured with appropriate permissions
2. **Java 17** installed
3. **Maven 3.8+** installed
4. **AWS SAM CLI** (optional but recommended)

## Key Features

This RAG API now uses **Spring AI** libraries instead of raw AWS SDKs, providing:
- Simplified configuration and setup
- Built-in vector store integration
- Automatic embedding generation
- Streamlined chat model interaction
- Better error handling and retry mechanisms

## Required AWS Permissions

Your IAM role/user needs the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:ListFoundationModels"
      ],
      "Resource": "*"# RAG API Deployment Guide

## Prerequisites

1. **AWS CLI** configured with appropriate permissions
2. **Java 17** installed
3. **Maven 3.8+** installed
4. **AWS SAM CLI** (optional but recommended)

## Required AWS Permissions

Your IAM role/user needs the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:ListFoundationModels"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "es:ESHttpGet",
        "es:ESHttpPost",
        "es:ESHttpPut",
        "es:ESHttpDelete"
      ],
      "Resource": "arn:aws:es:*:*:domain/your-opensearch-domain/*"
    }
  ]
}
```

## Building the Application

1. **Build the JAR file:**
   ```bash
   mvn clean package
   ```

2. **The shaded JAR will be created at:**
   ```
   target/rag-api-1.0.0-aws.jar
   ```

## Spring AI Configuration

The application now uses Spring AI auto-configuration. Key benefits:

### 1. **Simplified Bedrock Integration**
- No manual AWS SDK client creation
- Automatic credential handling
- Built-in retry and error handling

### 2. **Vector Store Integration**
- Native OpenSearch vector store support
- Automatic embedding generation during search
- Built-in similarity search capabilities

### 3. **Chat Model Integration**
- Simplified prompt templates
- Automatic model configuration
- Better response handling

## Environment Variables

Set these environment variables in your Lambda function:

| Variable | Description | Example |
|----------|-------------|---------|
| `OPENSEARCH_URL` | OpenSearch cluster URL | `https://search-docs.us-east-1.es.amazonaws.com` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `BEDROCK_EMBEDDING_MODEL` | Bedrock embedding model ID | `amazon.titan-embed-text-v1` |
| `BEDROCK_CLAUDE_MODEL` | Bedrock Claude model ID | `anthropic.claude-3-sonnet-20240229-v1:0` |
| `OPENSEARCH_INDEX` | OpenSearch index name | `documents` |
| `OPENSEARCH_USERNAME` | OpenSearch username (if auth enabled) | `admin` |
| `OPENSEARCH_PASSWORD` | OpenSearch password (if auth enabled) | `password` |

## API Endpoints

### 1. **Query Endpoint** - `POST /api/v1/rag/query`
```json
{
  "question": "What is machine learning?",
  "maxResults": 5,
  "threshold": 0.7,
  "filters": [
    {
      "field": "category",
      "value": "technology",
      "operator": "EQUALS"
    }
  ]
}
```

### 2. **Document Management**

**Index Single Document** - `POST /api/v1/documents/index`
```json
{
  "content": "This is the document content...",
  "metadata": {
    "title": "Document Title",
    "author": "Author Name",
    "category": "technology",
    "tags": ["ai", "ml"]
  }
}
```

**Index Multiple Documents** - `POST /api/v1/documents/index/batch`
```json
{
  "documents": [
    {
      "id": "doc-1",
      "content": "First document content...",
      "metadata": {"title": "Doc 1"}
    },
    {
      "id": "doc-2", 
      "content": "Second document content...",
      "metadata": {"title": "Doc 2"}
    }
  ]
}
```

**Delete Document** - `DELETE /api/v1/documents/{documentId}`

**Delete Multiple Documents** - `DELETE /api/v1/documents/batch`
```json
{
  "documentIds": ["doc-1", "doc-2", "doc-3"]
}
```

### 3. **Health & Info Endpoints**
- Health Check: `GET /api/v1/rag/health`
- API Info: `GET /api/v1/rag/info`

## Deployment Options

### Option 1: AWS SAM (Recommended)

1. **Deploy using SAM:**
   ```bash
   sam build
   sam deploy --guided --parameter-overrides \
     OpenSearchEndpoint=https://your-opensearch-endpoint.region.es.amazonaws.com \
     Environment=prod
   ```

### Option 2: AWS CLI Direct

1. **Create Lambda function:**
   ```bash
   aws lambda create-function \
     --function-name rag-api-spring-ai \
     --runtime java17 \
     --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
     --handler com.example.ragapi.lambda.LambdaHandler::handleRequest \
     --zip-file fileb://target/rag-api-1.0.0-aws.jar \
     --timeout 300 \
     --memory-size 1024 \
     --environment Variables='{
       "OPENSEARCH_URL":"https://your-opensearch-endpoint.region.es.amazonaws.com",
       "AWS_REGION":"us-east-1",
       "SPRING_PROFILES_ACTIVE":"prod"
     }'
   ```

## OpenSearch Index Setup

Spring AI will automatically create the index, but you can pre-configure it:

```json
{
  "mappings": {
    "properties": {
      "content": {
        "type": "text",
        "analyzer": "standard"
      },
      "embedding": {
        "type": "knn_vector",
        "dimension": 1536,
        "method": {
          "name": "hnsw",
          "space_type": "cosinesimil",
          "engine": "nmslib"
        }
      },
      "metadata": {
        "type": "object",
        "properties": {
          "title": { "type": "text" },
          "author": { "type": "keyword" },
          "source": { "type": "keyword" },
          "category": { "type": "keyword" },
          "createdAt": { "type": "date" },
          "tags": { "type": "keyword" }
        }
      }
    }
  },
  "settings": {
    "index": {
      "knn": true,
      "knn.algo_param.ef_search": 100
    }
  }
}
```

## Testing the API

### Health Check
```bash
curl https://your-api-gateway-url/api/v1/rag/health
```

### Index a Document
```bash
curl -X POST https://your-api-gateway-url/api/v1/documents/index \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring AI is a framework that simplifies AI integration in Spring applications.",
    "metadata": {
      "title": "Spring AI Overview",
      "category": "technology",
      "tags": ["spring", "ai", "framework"]
    }
  }'
```

### Query Example
```bash
curl -X POST https://your-api-gateway-url/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is Spring AI?",
    "maxResults": 3,
    "threshold": 0.7
  }'
```

## Spring AI Benefits

### 1. **Simplified Configuration**
- Auto-configuration for Bedrock services
- No manual client creation
- Environment-based configuration

### 2. **Better Error Handling**
- Built-in retry mechanisms
- Automatic fallback strategies
- Comprehensive error messages

### 3. **Enhanced Features**
- Automatic text chunking
- Smart prompt templates
- Vector store abstraction

### 4. **Development Experience**
- Less boilerplate code
- Better testing support
- Spring Boot integration

## Performance Optimization

1. **Cold Start Optimization:**
   - Spring AI lazy initialization reduces startup time
   - Use provisioned concurrency for consistent performance
   
2. **Memory Configuration:**
   - Start with 1024MB, Spring AI is memory efficient
   - Monitor actual usage and adjust accordingly
   
3. **Caching:**
   - Spring AI includes built-in caching for embeddings
   - Consider Redis for distributed caching

## Security Best Practices

1. **IAM Roles:** Use least privilege principle
2. **VPC:** Deploy Lambda in VPC if OpenSearch is in VPC
3. **API Gateway:** Implement authentication (API keys, Cognito, etc.)
4. **Encryption:** Enable encryption at rest and in transit
5. **Environment Variables:** Use AWS Systems Manager Parameter Store for sensitive configs

## Monitoring and Observability

1. **CloudWatch Logs:** Enhanced logging with Spring AI context
2. **Metrics:** Built-in Spring Boot Actuator metrics
3. **Tracing:** X-Ray integration for request tracing
4. **Health Checks:** Comprehensive health endpoints

## Troubleshooting

Common issues and solutions:

1. **Spring AI Configuration Issues:** Check `application.yml` syntax and environment variables
2. **Bedrock Access Denied:** Verify IAM permissions and region settings
3. **OpenSearch Connection:** Check URL format and authentication
4. **Memory Issues:** Increase Lambda memory or optimize batch sizes
5. **Cold Starts:** Use provisioned concurrency or reduce initialization overhead

## Migration from AWS SDK

If migrating from raw AWS SDK implementation:

1. **Replace AWS SDK dependencies** with Spring AI starters
2. **Update configuration** to use Spring AI properties
3. **Simplify service classes** using Spring AI abstractions
4. **Update tests** to use Spring AI test utilities
5. **Validate functionality** with existing data
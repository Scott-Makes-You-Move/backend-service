package nl.optifit.backendservice.configuration;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.cosmosdb.CosmosDBVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.cosmosdb.database-name}")
    private String databaseName;

    @Bean("filesVectorStore")
    public VectorStore filesVectorStore(CosmosAsyncClient cosmosClient, EmbeddingModel embeddingModel) {
        return CosmosDBVectorStore.builder(cosmosClient, embeddingModel)
                .databaseName(databaseName)
                .containerName("files")
                .metadataFields(List.of("accountId"))
                .build();
    }

    @Bean("chunksVectorStore")
    public VectorStore chunksVectorStore(CosmosAsyncClient cosmosClient, EmbeddingModel embeddingModel) {
        return CosmosDBVectorStore.builder(cosmosClient, embeddingModel)
                .databaseName(databaseName)
                .containerName("chunks")
                .metadataFields(List.of("topic", "subtopic"))
                .build();
    }
}

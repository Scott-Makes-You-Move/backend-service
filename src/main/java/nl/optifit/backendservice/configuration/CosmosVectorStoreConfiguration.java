package nl.optifit.backendservice.configuration;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.cosmosdb.CosmosDBVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class CosmosVectorStoreConfiguration {

    public static final String CHUNKS = "chunks";
    public static final String FILES = "files";

    private final CosmosVectorStoreProperties properties;
    private final EmbeddingModel embeddingModel;

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        return new CosmosClientBuilder()
                .endpoint(properties.getEndpoint())
                .key(properties.getKey())
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildAsyncClient();
    }

    @Bean(name = "chunksVectorStore")
    public VectorStore chunksVectorStore(CosmosAsyncClient client) {
        var cfg = properties.getContainers().get(CHUNKS);

        return CosmosDBVectorStore.builder(client, embeddingModel)
                .databaseName(properties.getDatabaseName())
                .containerName(cfg.getContainerName())
                .partitionKeyPath(cfg.getPartitionKeyPath())
                .metadataFields(List.of(cfg.getMetadataFields().split(",")))
                .vectorDimensions(cfg.getVectorDimensions())
                .vectorStoreThroughput(cfg.getVectorStoreThroughput())
                .build();
    }

    @Bean(name = "filesVectorStore")
    public VectorStore filesVectorStore(CosmosAsyncClient client) {
        var cfg = properties.getContainers().get(FILES);

        return CosmosDBVectorStore.builder(client, embeddingModel)
                .databaseName(properties.getDatabaseName())
                .containerName(cfg.getContainerName())
                .partitionKeyPath(cfg.getPartitionKeyPath())
                .metadataFields(List.of(cfg.getMetadataFields().split(",")))
                .vectorDimensions(cfg.getVectorDimensions())
                .vectorStoreThroughput(cfg.getVectorStoreThroughput())
                .build();
    }
}

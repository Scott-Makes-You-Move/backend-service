package nl.optifit.backendservice.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.cosmosdb")
public class CosmosVectorStoreProperties {
    private String endpoint;
    private String key;
    private String databaseName;
    private Map<String, ContainerConfig> containers;

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class ContainerConfig {
        private String containerName;
        private String partitionKeyPath;
        private String metadataFields;
        private Integer vectorStoreThroughput;
        private Long vectorDimensions;
    }
}

package nl.optifit.backendservice.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCosmosRepositories(basePackages = "nl.optifit.backendservice.repository.cosmos")
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    @Value("${spring.cloud.azure.cosmos.endpoint}")
    private String endpoint;
    @Value("${spring.cloud.azure.cosmos.key}")
    private String key;
    @Value("${spring.cloud.azure.cosmos.database}")
    private String databaseName;

    @Bean
    public CosmosClientBuilder getCosmosClientBuilder() {
        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key);
    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(true)
                .build();
    }

    @Override
    protected String getDatabaseName() {
        return databaseName != null ? databaseName : "default";
    }
}

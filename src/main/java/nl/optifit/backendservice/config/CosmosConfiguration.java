package nl.optifit.backendservice.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableCosmosRepositories
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    //    @Value("${CONFIGURATION__AZURECOSMOSDB__ENDPOINT}")
    private final String endpoint = "https://smym-cosmos-nosql.documents.azure.com:443/";

    //    @Value("${CONFIGURATION__AZURECOSMOSDB__DATABASENAME}")
    private final String databaseName = "products";

    @Bean
    public CosmosClientBuilder getCosmosClientBuilder() {
        return new CosmosClientBuilder()
                .endpoint("https://smym-cosmos-nosql.documents.azure.com:443/")
                .key("KzZi7PBx8WAVswmm38nmwCr39wYHbmIRFBVcyjOL157dyvAFHbS6QxhZ0DJffPOhrYOqIz0uTN9zACDbnafUug==");
    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(true)
                .build();
    }

    @Override
    protected String getDatabaseName() {
        return databaseName != null ? databaseName : "cosmicworks";
    }
}

package nl.optifit.backendservice.configuration;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosmosClientConfig {

    @Value("${spring.ai.vectorstore.cosmosdb.endpoint}")
    private String endpoint;
    @Value("${spring.ai.vectorstore.cosmosdb.key}")
    private String key;

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        if (StringUtils.isBlank(endpoint)) {
            throw new IllegalStateException("Missing required property: endpoint");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalStateException("Missing required property: key");
        }

        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .buildAsyncClient();
    }
}

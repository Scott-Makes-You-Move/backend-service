package nl.optifit.backendservice;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.cosmosdb.autoconfigure.CosmosDBVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(
        exclude = {
                CosmosDBVectorStoreAutoConfiguration.class
        }
)
class BackendServiceApplicationTests {

    @Test
    void contextLoads() {
        // only loads spring context
    }

}

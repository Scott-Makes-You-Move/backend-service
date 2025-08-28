package nl.optifit.backendservice;

import nl.optifit.backendservice.configuration.AiConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.cosmosdb.autoconfigure.CosmosDBVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.observation.autoconfigure.VectorStoreObservationAutoConfiguration;
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

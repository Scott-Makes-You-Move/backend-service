package nl.optifit.backendservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.cosmosdb.autoconfigure.CosmosDBVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication(
        exclude = {
                CosmosDBVectorStoreAutoConfiguration.class,
                ErrorMvcAutoConfiguration.class
        }
)
@EnableScheduling
public class BackendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendServiceApplication.class, args);
    }

}

package nl.optifit.backendservice.configuration;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphClientConfiguration {

    public static final String SCOPE = "https://graph.microsoft.com/.default";

    @Value("${microsoft.entra.id.client-id}")
    private String clientId;
    @Value("${microsoft.entra.id.client-secret}")
    private String clientSecret;
    @Value("${microsoft.entra.id.tenant-id}")
    private String tenantId;

    @Bean
    public GraphServiceClient graphServiceClient() {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        return new GraphServiceClient(clientSecretCredential, SCOPE);
    }
}

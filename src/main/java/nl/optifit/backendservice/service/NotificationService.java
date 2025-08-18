package nl.optifit.backendservice.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Value("${microsoft.entra.id.client-id}")
    private String clientId;
    @Value("${microsoft.entra.id.client-secret}")
    private String clientSecret;
    @Value("${microsoft.entra.id.tenant-id}")
    private String tenantId;

    public void sendNotification() {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                Collections.singletonList("https://graph.microsoft.com/.default"),
                clientSecretCredential
        );

        GraphServiceClient<?> graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(authProvider)
                .buildClient();

        UserCollectionPage usersPage = graphClient
                .users()
                .buildRequest()
                .get();

        List<User> users = usersPage.getCurrentPage();
        for (User user : users) {
            log.debug("displayName '%s', email '%s', id '%s'".formatted(user.displayName, user.mail, user.id));
        }
    }
}

package nl.optifit.backendservice.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        GraphServiceClient graphServiceClient = new GraphServiceClient(
                credential, "https://graph.microsoft.com/.default");

        Event event = new Event();
        event.setSubject("Daily workout");

        Event createdEvent = graphServiceClient.users().byUserId("deroosean@gmail.com").events().post(event);

        log.debug("Event created: {}", createdEvent.getId());
    }
}

package nl.optifit.backendservice.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Attendee;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.ItemBody;
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
    @Value("${microsoft.entra.id.user}")
    private String user;

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

        Event event = new Event();
        ItemBody body = new ItemBody();
        body.content = "<p>It's time for you your 1 min movement break!</p>";
        body.contentType = BodyType.HTML;

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.address = "seanderoo@hotmail.com";
        emailAddress.name = "Seán de Roo";

        Attendee attendee = new Attendee();
        attendee.emailAddress = emailAddress;

        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = "2025-08-26T15:00:00.0000000";
        start.timeZone = "Europe/Amsterdam";
        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = "2025-08-26T16:00:00.0000000";
        end.timeZone = "Europe/Amsterdam";

        event.subject = "Make your next move";
        event.body = body;
        event.start = start;
        event.end = end;
        event.attendees = List.of(attendee);

        Event postedEvent = graphClient
                .users(user)
                .calendar()
                .events()
                .buildRequest()
                .post(event);

        log.info("Posted event: {}", postedEvent.id);

    }
}

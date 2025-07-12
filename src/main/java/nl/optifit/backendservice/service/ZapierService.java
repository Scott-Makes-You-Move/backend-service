package nl.optifit.backendservice.service;

import jakarta.ws.rs.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.zapier.NotificationDto;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.util.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ZapierService {

    @Value("${webhooks.zapier.notification}")
    private String notificationWebhookUrl;

    private final WebClient webClient;
    private final KeycloakService keycloakService;

    public ResponseEntity<String> sendNotification(Session newSession) {
        UserRepresentation userRepresentation = keycloakService.findUserById(newSession.getAccount().getId())
                .orElseThrow(() -> new NotFoundException("Could not find user"))
                .toRepresentation();

        NotificationDto notificationDto = NotificationDto.fromUserSession(userRepresentation, newSession);

        return webClient.post()
                .uri(notificationWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(notificationDto)
                .retrieve()
                .toEntity(String.class)
                .block();
    }
}

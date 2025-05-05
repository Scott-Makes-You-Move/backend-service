package nl.optifit.backendservice.service;

import jakarta.ws.rs.*;
import lombok.*;
import nl.optifit.backendservice.dto.*;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.util.*;
import org.keycloak.representations.idm.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;

@RequiredArgsConstructor
@Service
public class ZapierService {

    private final WebClient webClient;
    private final KeycloakService keycloakService;

    public ResponseEntity<String> sendNotification(Session newSession) {
        UserRepresentation userRepresentation = keycloakService.findUserById(newSession.getAccount().getId())
                .orElseThrow(() -> new NotFoundException("Could not find user"))
                .toRepresentation();

        ZapierWorkflowDto zapierWorkflowDto = ZapierWorkflowDto.fromUserSession(userRepresentation, newSession);

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(zapierWorkflowDto)
                .retrieve()
                .toEntity(String.class)
                .block();
    }
}

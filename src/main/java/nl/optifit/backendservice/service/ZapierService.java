package nl.optifit.backendservice.service;

import jakarta.ws.rs.*;
import lombok.*;
import nl.optifit.backendservice.dto.zapier.ReceiveChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.NotificationDto;
import nl.optifit.backendservice.dto.zapier.InitiateChatbotConversationDto;
import nl.optifit.backendservice.dto.zapier.ZapierWorkflowResponseDto;
import nl.optifit.backendservice.model.*;
import nl.optifit.backendservice.util.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;

@RequiredArgsConstructor
@Service
public class ZapierService {

    @Value("${webhooks.zapier.notification}")
    private String notificationWebhookUrl;
    @Value("${webhooks.zapier.chatbot}")
    private String chatbotWebhookUrl;
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

    public ResponseEntity<ZapierWorkflowResponseDto> initiateChatbotConversation(InitiateChatbotConversationDto initiateChatbotConversationDto) {
        return webClient.post()
                .uri(chatbotWebhookUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJYb3JTUkZVSXE4UUhTM3lUTmx0TDQxNnNSUW5PblJkRHE3RUJqOUx3WTNjIn0.eyJleHAiOjE3NDkyMzUzNzcsImlhdCI6MTc0OTIzNTA3NywiYXV0aF90aW1lIjoxNzQ5MjMzMjYyLCJqdGkiOiI2NjU1ODhjNC1lYjc5LTQ0MmItYTI4My03MTI1ZDk3MjcwNTciLCJpc3MiOiJodHRwczovL3NteW0ta2V5Y2xvYWstZ2RhZGNkaGFiOGR5ZGdjei53ZXN0ZXVyb3BlLTAxLmF6dXJld2Vic2l0ZXMubmV0L3JlYWxtcy9zbXltLWRldiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJlZTgxOGFjNi1jNDIwLTRkZTUtOWFkZC00YTBiNmRlNWI3YzAiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJldmVudC1saXN0ZW5lci1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiOTA4ZWZjZWMtNjRmMy00ZmY3LWI0YWItZDYzM2JlZjMyOWM5IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLXNteW0tZGV2Iiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJzaWQiOiI5MDhlZmNlYy02NGYzLTRmZjctYjRhYi1kNjMzYmVmMzI5YzkiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZ2VuZGVyIjoiTWFsZSIsIm5hbWUiOiJNYWxjb2xtIEtlbnRlIiwicHJlZmVycmVkX3VzZXJuYW1lIjoibWFsY29sbSIsImdpdmVuX25hbWUiOiJNYWxjb2xtIiwiZmFtaWx5X25hbWUiOiJLZW50ZSIsImVtYWlsIjoibWFsY29sbS5rZW50ZUBnbWFpbC5jb20ifQ.MZ_prlTb5-Ymv74hpqBuU8xNdAQmR5w98yjoU83Rp9yH-AFyE2BDNQ5bJGDcrpE4BJASdT-zCwBwWAxfUBtOU0rb8-NeJLVltVa6J-oXqfNdmmLyKwMXt_VNUdPcuo8xItAmkILXALdEueCWs3KsRczJY44xYGXA9FfjYAd-ym87wuvLhV-Bow2nERJuv4fKDRizoHRiTaMzEDlQn2Y8ag-lorMtK_E7bydxbCZUxOLJZM0aNJbJ2CtF-AZlRNeaIxmueM9lKoipM1ArW_QOT1kpJp2xExtvNsa7932jlDq-fXJdb2DmuBhAZ693rknJbfnb4gRS1zdDy2nU8KifCA") 
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(initiateChatbotConversationDto)
                .retrieve()
                .toEntity(ZapierWorkflowResponseDto.class)
                .block();
    }
}

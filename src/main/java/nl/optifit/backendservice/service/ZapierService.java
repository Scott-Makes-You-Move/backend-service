package nl.optifit.backendservice.service;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.zapier.ChatbotResponseDto;
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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ZapierService {

    @Value("${webhooks.zapier.notification}")
    private String notificationWebhookUrl;
    @Value("${webhooks.zapier.chatbot}")
    private String chatbotWebhookUrl;

    private final WebClient webClient;
    private final KeycloakService keycloakService;
    private final ConcurrentHashMap<String, String> responseMap = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 20;
    private static final Duration DELAY = Duration.ofSeconds(1);

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

    public Mono<ResponseEntity<ChatbotResponseDto>> initiateChatbotConversation(InitiateChatbotConversationDto initiateDto, HttpServletRequest request) {
        String sessionId = initiateDto.getSessionId();

        log.info("Initiating chatbot conversation at {}", ZonedDateTime.now());

        return webClient.post()
                .uri(chatbotWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", request.getHeader("Authorization"))
                .bodyValue(initiateDto)
                .retrieve()
                .toBodilessEntity()
                .then(waitForZapierResponse(sessionId))
                .map(ResponseEntity::ok);
    }

    public void storeResponse(ChatbotResponseDto chatbotResponseDto) {
        log.info("Received chatbot response: '{}'", chatbotResponseDto.getAiResponse());
        responseMap.put(chatbotResponseDto.getSessionId(), chatbotResponseDto.getAiResponse());
    }

    private Mono<ChatbotResponseDto> waitForZapierResponse(String sessionId) {
        return Mono.defer(() -> {
                    String response = responseMap.get(sessionId);
                    if (response != null) {
                        log.info("Chatbot response found for session '{}'", sessionId);
                        responseMap.remove(sessionId); // Optional: cleanup
                        return Mono.just(new ChatbotResponseDto(sessionId, response));
                    }
                    return Mono.error(new IllegalStateException("Response not ready"));
                })
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, DELAY)
                        .filter(IllegalStateException.class::isInstance)
                )
                .onErrorResume(e -> {
                    log.warn("Zapier response timeout or error: {}", e.getMessage());
                    return Mono.just(new ChatbotResponseDto(sessionId, "⚠️ No response in time."));
                });
    }
}

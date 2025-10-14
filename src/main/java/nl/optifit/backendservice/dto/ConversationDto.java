package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ConversationDto(@JsonProperty("session_id") UUID sessionId,
                              @JsonProperty("user_message") String userMessage) {
}

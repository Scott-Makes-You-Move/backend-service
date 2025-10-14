package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ChatbotResponseDto(@JsonProperty("session_id") UUID sessionId,
                                 @JsonProperty("ai_response") String aiResponse) {
}

package nl.optifit.backendservice.dto.zapier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotResponseDto {
    @JsonProperty("session_id")
    private UUID sessionId;
    @JsonProperty("ai_response")
    private String aiResponse;
}

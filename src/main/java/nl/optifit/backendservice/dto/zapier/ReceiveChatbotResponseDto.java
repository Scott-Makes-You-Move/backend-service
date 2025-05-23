package nl.optifit.backendservice.dto.zapier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiveChatbotResponseDto {
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("ai_response")
    private String aiResponse;
}

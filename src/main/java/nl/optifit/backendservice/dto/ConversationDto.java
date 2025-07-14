package nl.optifit.backendservice.dto;

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
public class ConversationDto {
    @JsonProperty("session_id")
    private UUID sessionId;
    @JsonProperty("user_message")
    private String userMessage;
}

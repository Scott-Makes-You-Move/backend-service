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
public class ZapierWorkflowResponseDto {
    private UUID attempt;
    private UUID id;
    @JsonProperty("request_id")
    private UUID requestId;
    private String status;
}

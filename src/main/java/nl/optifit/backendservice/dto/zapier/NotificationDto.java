package nl.optifit.backendservice.dto.zapier;

import lombok.*;
import nl.optifit.backendservice.model.*;
import org.keycloak.representations.idm.*;

import java.util.UUID;
import java.util.regex.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDto {
    private String name;
    private String email;
    private String startTime;
    private String endTime;
    private UUID sessionId;

    public static NotificationDto fromUserSession(UserRepresentation userRepresentation, Session newSession) {
        return NotificationDto.builder()
                .name(userRepresentation.getFirstName())
                .email(userRepresentation.getEmail())
                .startTime(newSession.getSessionStart().toLocalTime().toString())
                .endTime(newSession.getSessionStart().plusHours(1).toLocalTime().toString())
                .sessionId(newSession.getId())
                .build();
    }
}

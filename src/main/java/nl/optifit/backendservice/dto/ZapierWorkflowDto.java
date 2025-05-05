package nl.optifit.backendservice.dto;

import lombok.*;
import nl.optifit.backendservice.model.*;
import org.keycloak.representations.idm.*;

import java.time.*;
import java.util.regex.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZapierWorkflowDto {
    private String name;
    private String email;
    private String startTime;
    private String endTime;
    private String videoId;

    public static ZapierWorkflowDto fromUserSession(UserRepresentation userRepresentation, Session newSession) {
        return ZapierWorkflowDto.builder()
                .name(userRepresentation.getFirstName())
                .email(userRepresentation.getEmail())
                .startTime(newSession.getSessionStart().toLocalTime().toString())
                .endTime(newSession.getSessionStart().plusHours(1).toLocalTime().toString())
                .videoId(getVideoIdFromUrl(newSession.getExerciseVideo().getVideoUrl()))
                .build();
    }

    private static String getVideoIdFromUrl(String videoUrl) {
        String pattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(videoUrl);

        return matcher.find() ? matcher.group(1) : null;
    }
}

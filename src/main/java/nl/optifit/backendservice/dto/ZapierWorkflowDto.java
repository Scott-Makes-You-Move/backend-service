package nl.optifit.backendservice.dto;

import lombok.*;
import nl.optifit.backendservice.model.*;
import org.keycloak.representations.idm.*;

import java.util.regex.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZapierWorkflowDto {
    private String name;
    private String email;
    private String exerciseType;
    private String videoId;

    public static ZapierWorkflowDto fromUserSession(UserRepresentation userRepresentation, Session session) {
        return ZapierWorkflowDto.builder()
                .name(userRepresentation.getFirstName())
                .email(userRepresentation.getEmail())
                .exerciseType(session.getExerciseType().getDisplayName().toLowerCase())
                .videoId(getVideoIdFromUrl(session.getExerciseVideo().getVideoUrl()))
                .build();
    }

    private static String getVideoIdFromUrl(String videoUrl) {
        String pattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(videoUrl);

        return matcher.find() ? matcher.group(1) : null;
    }
}

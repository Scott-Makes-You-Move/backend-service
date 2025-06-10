package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import nl.optifit.backendservice.model.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam")
    private ZonedDateTime sessionStartTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam")
    private ZonedDateTime sessionExecutionTime;
    private ExerciseType exerciseType;
    private SessionStatus sessionStatus;
    private String sessionVideoUrl;

    public static SessionDto fromSession(Session session) {
        return SessionDto.builder()
                .sessionStartTime(session.getSessionStart())
                .sessionExecutionTime(session.getSessionExecutionTime())
                .exerciseType(session.getExerciseType())
                .sessionStatus(session.getSessionStatus())
                .sessionVideoUrl(Objects.isNull(session.getExerciseVideo()) ? null : session.getExerciseVideo().getVideoUrl())
                .build();
    }
}

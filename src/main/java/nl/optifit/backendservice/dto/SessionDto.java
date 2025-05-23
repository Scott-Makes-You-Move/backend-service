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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private ZonedDateTime sessionStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private ZonedDateTime sessionExecutionTime;
    private ExerciseType exerciseType;
    private SessionStatus sessionStatus;
    private String sessionVideoUrl;

    public static SessionDto fromSession(Session session) {
        return SessionDto.builder()
                .sessionStartTime(session.getSessionStart().atZone(ZoneId.of("Europe/Amsterdam")))
                .sessionExecutionTime(session.getSessionExecutionTime().atZone(ZoneId.of("Europe/Amsterdam")))
                .exerciseType(session.getExerciseType())
                .sessionStatus(session.getSessionStatus())
                .sessionVideoUrl(Objects.isNull(session.getExerciseVideo()) ? null : session.getExerciseVideo().getVideoUrl())
                .build();
    }
}

package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.model.Session;
import nl.optifit.backendservice.model.SessionStatus;

import java.time.ZonedDateTime;
import java.util.Objects;

public record SessionDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam") ZonedDateTime sessionStartTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam") ZonedDateTime sessionExecutionTime,
        ExerciseType exerciseType,
        SessionStatus sessionStatus,
        String sessionVideoUrl) {

    public static SessionDto fromSession(Session session) {
        return new SessionDto(
                session.getSessionStart(),
                session.getSessionExecutionTime(),
                session.getExerciseType(),
                session.getSessionStatus(),
                session.getExerciseVideo() == null ? null : session.getExerciseVideo().getVideoUrl()
        );
    }
}

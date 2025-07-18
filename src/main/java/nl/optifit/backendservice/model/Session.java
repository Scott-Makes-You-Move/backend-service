package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions", indexes = @Index(columnList = "account_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
    private Account account;
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam")
    private ZonedDateTime sessionStart;
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Europe/Amsterdam")
    private ZonedDateTime sessionExecutionTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseType exerciseType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus sessionStatus;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_video_id", referencedColumnName = "id")
    private ExerciseVideo exerciseVideo;
}

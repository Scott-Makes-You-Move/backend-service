package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification {
    @Id
    private UUID sessionId;
    @NotBlank
    private String title;
    @NotBlank
    private String linkToVideo;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @MapsId
    @JsonBackReference
    private Session session;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private LocalDateTime expiresAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusHours(1);
    private Boolean pushed = false;
    private LocalDateTime pushedAt;
}
package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDateTime;
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
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}
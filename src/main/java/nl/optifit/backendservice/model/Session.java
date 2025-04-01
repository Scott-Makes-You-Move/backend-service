package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session", indexes = @Index(columnList = "accountId"))
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
    @JoinColumn(name = "accountId", referencedColumnName = "accountId", nullable = false)
    @JsonIgnore
    private Account account;
    @PastOrPresent(message = "Measured date cannot be in the future")
    private LocalDateTime sessionStart;
    @PastOrPresent(message = "Measured date cannot be in the future")
    private LocalDateTime sessionExecutionTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseType exerciseType;
    @Column(nullable = false)
    private boolean sessionCompleted;
}

package nl.optifit.backendservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mobility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mobility {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String accountId;
    @ManyToOne
    @JoinColumn(name = "progress_id", nullable = false)
    private Progress progress;
    private LocalDateTime measuredOn;
    @Min(1)
    @Max(3)
    private int shoulder;
    @Min(1)
    @Max(3)
    private int back;
    @Min(1)
    @Max(3)
    private int hip;
}

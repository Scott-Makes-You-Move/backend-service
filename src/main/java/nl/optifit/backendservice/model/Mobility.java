package nl.optifit.backendservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mobility", indexes = @Index(columnList = "accountId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mobility implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne
    @JoinColumn(name = "progress_id", nullable = false)
    private Progress progress;
    @PastOrPresent(message = "Measured date cannot be in the future")
    private LocalDateTime measuredOn;
    @Min(1) @Max(3)
    private Integer shoulder;
    @Min(1) @Max(3)
    private Integer back;
    @Min(1) @Max(3)
    private Integer hip;
}

package nl.optifit.backendservice.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "biometrics", indexes = @Index(columnList = "accountId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biometrics implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
    @JsonIgnore
    private Account account;
    @PastOrPresent(message = "Measured date cannot be in the future")
    private LocalDateTime measuredOn;
    @Min(value = 0, message = "Weight must be positive")
    private Double weight;
    @Min(value = 1, message = "Fat percentage must be positive")
    @Max(value = 59, message = "Fat percentage cannot exceed 100%")
    private Double fat;
    @Min(value = 1, message = "Visceral fat must be at least 1")
    @Max(value = 59, message = "Visceral fat cannot exceed 59")
    private Integer visceralFat;
}

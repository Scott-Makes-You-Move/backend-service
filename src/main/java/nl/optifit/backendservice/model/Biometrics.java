package nl.optifit.backendservice.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "biometrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biometrics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String accountId;
    @ManyToOne
    @JoinColumn(name = "progress_id", nullable = false)
    private Progress progress;
    private LocalDateTime measuredOn;
    private double weight;
    private double fat;
    private int visceralFat;
}

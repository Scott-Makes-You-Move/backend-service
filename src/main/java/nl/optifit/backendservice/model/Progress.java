package nl.optifit.backendservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String accountId;
    @OneToMany(mappedBy = "accountId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "biometrics")
    private List<Biometrics> biometrics = new ArrayList<>();
    @OneToMany(mappedBy = "accountId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "mobilities")
    private List<Mobility> mobilities = new ArrayList<>();
}
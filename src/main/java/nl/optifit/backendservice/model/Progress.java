package nl.optifit.backendservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "progress", indexes = @Index(columnList = "accountId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @OneToMany(mappedBy = "progress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "biometrics")
    private List<Biometrics> biometrics = new ArrayList<>();
    @OneToMany(mappedBy = "progress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "mobilities")
    private List<Mobility> mobilities = new ArrayList<>();
}
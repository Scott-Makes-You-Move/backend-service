package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "leaderboard", indexes = @Index(columnList = "accountId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard implements Serializable {
    @Id
    @Column(name = "account_id")
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @JsonBackReference
    private Account account;
    @Column(nullable = false)
    private Double completionRate;
    @Column(nullable = false)
    private Integer currentStreak;
    @Column(nullable = false)
    private Integer longestStreak;
}
package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboards", indexes = @Index(columnList = "account_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard implements Serializable {
    @Id
    private String accountId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @MapsId
    @JsonBackReference
    private Account account;
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated = LocalDateTime.now();
    @Column(nullable = false)
    private Double completionRate;
    @Column(nullable = false)
    private Integer currentStreak;
    @Column(nullable = false)
    private Integer longestStreak;
}

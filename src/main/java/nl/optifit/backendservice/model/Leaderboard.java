package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Builder.Default
    @PastOrPresent(message = "Measured datetime cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated = LocalDateTime.now();
    @Builder.Default
    @PastOrPresent(message = "Reset datetime cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resetAt = LocalDateTime.now();
    @Column(nullable = false)
    @Builder.Default
    private Double completionRate = 0.0;
    @Column(nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;
    @Column(nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;
    @Column(nullable = false)
    @Builder.Default
    private Boolean recentWinner = false;
}

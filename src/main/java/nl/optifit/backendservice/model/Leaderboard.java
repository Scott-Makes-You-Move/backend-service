package nl.optifit.backendservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
public class Leaderboard {
    @Id
    private String id;
    private double streak;
}
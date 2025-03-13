package nl.optifit.backendservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@Entity
public class Progress {
    @Id
    private String id;
    private LocalDate date;
}
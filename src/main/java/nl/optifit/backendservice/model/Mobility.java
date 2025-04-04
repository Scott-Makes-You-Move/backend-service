package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "mobilities", indexes = @Index(columnList = "accountId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mobility implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
    @JsonIgnore
    private Account account;
    @PastOrPresent(message = "Measured date cannot be in the future")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDateTime measuredOn;
    @Min(1) @Max(3)
    private Integer shoulder;
    @Min(1) @Max(3)
    private Integer back;
    @Min(1) @Max(3)
    private Integer hip;
}

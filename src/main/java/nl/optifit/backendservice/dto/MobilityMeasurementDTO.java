package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Mobility;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties
public class MobilityMeasurementDTO {
    private LocalDate measuredOn;
    private Integer shoulder;
    private Integer back;
    private Integer hip;

    public static Mobility toMobility(Account account, MobilityMeasurementDTO mobilityMeasurementDTO) {
        return Mobility.builder()
                .account(account)
                .measuredOn(mobilityMeasurementDTO.getMeasuredOn())
                .shoulder(mobilityMeasurementDTO.getShoulder())
                .back(mobilityMeasurementDTO.getBack())
                .hip(mobilityMeasurementDTO.getHip())
                .build();
    }
}

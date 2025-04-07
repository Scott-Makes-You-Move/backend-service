package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Mobility;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties
public class MobilityMeasurementDto {
    private LocalDate measuredOn;
    private Integer shoulder;
    private Integer back;
    private Integer hip;

    public static Mobility toMobility(Account account, MobilityMeasurementDto mobilityMeasurementDTO) {
        return Mobility.builder()
                .account(account)
                .measuredOn(mobilityMeasurementDTO.getMeasuredOn())
                .shoulder(mobilityMeasurementDTO.getShoulder())
                .back(mobilityMeasurementDTO.getBack())
                .hip(mobilityMeasurementDTO.getHip())
                .build();
    }
}

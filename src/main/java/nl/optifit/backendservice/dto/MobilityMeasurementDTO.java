package nl.optifit.backendservice.dto;

import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;
import nl.optifit.backendservice.model.Mobility;

import java.time.LocalDateTime;

@Getter
@Setter
public class MobilityMeasurementDTO {
    private Integer shoulder;
    private Integer back;
    private Integer hip;

    public static Mobility toMobility(Account account, MobilityMeasurementDTO mobilityMeasurementDTO) {
        return Mobility.builder()
                .account(account)
                .measuredOn(LocalDateTime.now())
                .shoulder(mobilityMeasurementDTO.getShoulder())
                .back(mobilityMeasurementDTO.getBack())
                .hip(mobilityMeasurementDTO.getHip())
                .build();
    }
}

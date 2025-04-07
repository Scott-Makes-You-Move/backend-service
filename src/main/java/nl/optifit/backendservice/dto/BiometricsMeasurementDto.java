package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties
public class BiometricsMeasurementDto {
    private LocalDate measuredOn;
    private Double weight;
    private Double fat;
    private Integer visceralFat;

    public static Biometrics toBiometrics(Account account, BiometricsMeasurementDto biometricsMeasurementDTO) {
        return Biometrics.builder()
                .account(account)
                .measuredOn(biometricsMeasurementDTO.getMeasuredOn())
                .weight(biometricsMeasurementDTO.getWeight())
                .fat(biometricsMeasurementDTO.getFat())
                .visceralFat(biometricsMeasurementDTO.getVisceralFat())
                .build();
    }
}
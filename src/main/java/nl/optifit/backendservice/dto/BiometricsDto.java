package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;

import java.time.LocalDate;

public record BiometricsDto(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate measuredOn, double weight, double fat,
                            int visceralFat) {

    public static BiometricsDto fromBiometrics(Biometrics biometrics) {
        return new BiometricsDto(biometrics.getMeasuredOn(), biometrics.getWeight(), biometrics.getFat(), biometrics.getVisceralFat());
    }

    public static Biometrics toBiometrics(Account account, BiometricsDto biometricsDTO) {
        return Biometrics.builder()
                .account(account)
                .measuredOn(biometricsDTO.measuredOn())
                .weight(biometricsDTO.weight())
                .fat(biometricsDTO.fat())
                .visceralFat(biometricsDTO.visceralFat())
                .build();
    }
}

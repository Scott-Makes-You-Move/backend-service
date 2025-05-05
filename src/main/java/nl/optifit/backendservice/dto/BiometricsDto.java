package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Biometrics;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricsDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measuredOn;
    private Double weight;
    private Double fat;
    private Integer visceralFat;

    public static BiometricsDto fromBiometrics(Biometrics biometrics) {
        return BiometricsDto.builder()
                .measuredOn(biometrics.getMeasuredOn())
                .weight(biometrics.getWeight())
                .fat(biometrics.getFat())
                .visceralFat(biometrics.getVisceralFat())
                .build();
    }

    public static Biometrics toBiometrics(Account account, BiometricsDto biometricsDTO) {
        return Biometrics.builder()
                .account(account)
                .measuredOn(biometricsDTO.getMeasuredOn())
                .weight(biometricsDTO.getWeight())
                .fat(biometricsDTO.getFat())
                .visceralFat(biometricsDTO.getVisceralFat())
                .build();
    }
}

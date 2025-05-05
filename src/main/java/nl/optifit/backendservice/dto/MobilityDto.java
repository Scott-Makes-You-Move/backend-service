package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Mobility;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobilityDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measuredOn;
    private Integer shoulder;
    private Integer back;
    private Integer hip;

    public static MobilityDto fromMobility(Mobility mobility) {
        return MobilityDto.builder()
                .measuredOn(mobility.getMeasuredOn())
                .shoulder(mobility.getShoulder())
                .back(mobility.getBack())
                .hip(mobility.getHip())
                .build();
    }

    public static Mobility toMobility(Account account, MobilityDto mobilityDTO) {
        return Mobility.builder()
                .account(account)
                .measuredOn(mobilityDTO.getMeasuredOn())
                .shoulder(mobilityDTO.getShoulder())
                .back(mobilityDTO.getBack())
                .hip(mobilityDTO.getHip())
                .build();
    }
}

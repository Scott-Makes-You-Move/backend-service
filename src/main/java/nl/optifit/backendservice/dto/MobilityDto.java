package nl.optifit.backendservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.model.Mobility;

import java.time.LocalDate;

public record MobilityDto(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate measuredOn, int shoulder, int back, int hip) {

    public static MobilityDto fromMobility(Mobility mobility) {
        return new MobilityDto(mobility.getMeasuredOn(), mobility.getShoulder(), mobility.getBack(), mobility.getHip());
    }

    public static Mobility toMobility(Account account, MobilityDto mobilityDTO) {
        return Mobility.builder()
                .account(account)
                .measuredOn(mobilityDTO.measuredOn())
                .shoulder(mobilityDTO.shoulder())
                .back(mobilityDTO.back)
                .hip(mobilityDTO.hip())
                .build();
    }
}

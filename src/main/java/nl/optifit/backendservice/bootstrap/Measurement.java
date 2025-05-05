package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;

@Getter
@Setter
public class Measurement {
    @JsonProperty("measuredOn")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measuredOn;

    @JsonProperty("weight")
    private Double weight;

    @JsonProperty("fat")
    private Double fat;

    @JsonProperty("visceralFat")
    private Integer visceralFat;

    @JsonProperty("shoulder")
    private Integer shoulder;

    @JsonProperty("back")
    private Integer back;

    @JsonProperty("hip")
    private Integer hip;
}

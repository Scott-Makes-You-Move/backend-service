package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Measurement {
    @JsonProperty("measuredOn")
    private String measuredOn;

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

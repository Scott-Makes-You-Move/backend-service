package nl.optifit.backendservice.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.*;

import java.util.List;

@Getter
@Setter
public class BootstrapDataModel {
    @JsonProperty("username")
    private String username;

    @JsonProperty("measurements")
    private List<Measurement> measurements;

    @JsonProperty("leaderboard")
    private Leaderboard leaderboard;

    @JsonProperty("sessions")
    private List<Session> sessions;
}
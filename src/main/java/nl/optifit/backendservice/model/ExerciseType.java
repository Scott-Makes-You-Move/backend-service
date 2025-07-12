package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum ExerciseType {
    HIP("Hip"),
    SHOULDER("Shoulder"),
    BACK("Back");

    private final String displayName;

    @JsonCreator
    public static ExerciseType fromString(String value) {
        try {
            return ExerciseType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unknown exercise type: " + value);
            throw e;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

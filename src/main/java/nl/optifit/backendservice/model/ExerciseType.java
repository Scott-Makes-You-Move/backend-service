package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
public enum ExerciseType {
    HIP("Hip"),
    SHOULDER("Shoulder"),
    BACK("Back");

    private final String displayName;

    ExerciseType(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static ExerciseType fromString(String value) {
        try {
            return ExerciseType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle unknown values if needed
            return null;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
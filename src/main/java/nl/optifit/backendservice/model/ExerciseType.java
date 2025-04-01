package nl.optifit.backendservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExerciseType {
    HIP("Hip"),
    SHOULDER("Shoulder"),
    BACK("Back");

    private final String displayName;
}
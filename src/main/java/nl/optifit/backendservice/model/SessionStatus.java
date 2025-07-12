package nl.optifit.backendservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    NEW("New"),
    COMPLETED("Completed"),
    OVERDUE("Overdue");

    private final String displayName;

    @JsonCreator
    public static SessionStatus fromString(String value) {
        try {
            return SessionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unknown session status: " + value);
            throw e;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

package ai.timefold.solver.service.maps.api.model;

import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Schema(description = "The type of transport used (car, bicycle, ... ).")
public enum TransportType {

    CAR("car"),
    BICYCLE("bicycle"),
    FOOT("foot"),
    AUTO_SELECT("auto-select");

    /**
     * Every transport type that maps to an actual routing profile, i.e. all but {@link #AUTO_SELECT}.
     */
    public static final List<TransportType> ROUTING_PROFILES = List.of(CAR, BICYCLE, FOOT);

    private final String value;

    TransportType(String value) {
        Objects.requireNonNull(value, "TransportType value must not be null.");
        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TransportType value must not be blank.");
        }
        this.value = value;
    }

    @JsonCreator
    public static TransportType of(String value) {
        Objects.requireNonNull(value, "TransportType value must not be null.");
        // Both the wire value ("auto-select") and the enum name ("AUTO_SELECT") are accepted.
        return TransportType.valueOf(value.trim().toUpperCase().replace('-', '_'));
    }

    @JsonValue
    public String value() {
        return value;
    }

    public boolean isAutoSelect() {
        return this == AUTO_SELECT;
    }

    @Override
    public String toString() {
        return value;
    }
}

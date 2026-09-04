package ai.timefold.solver.service.maps.api.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Schema(description = "The type of transport used (car, bike, ... ) supported by Timefold.")
public enum TransportType {

    CAR("car");

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
        return TransportType.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

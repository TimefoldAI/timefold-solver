package ai.timefold.solver.service.maps.api.model;

import java.util.Objects;

public record TransportType(String value) {

    public static final TransportType CAR = new TransportType("car");

    public TransportType {
        Objects.requireNonNull(value, "TransportType value must not be null.");
        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TransportType value must not be blank.");
        }
    }

    public static TransportType of(String value) {
        return new TransportType(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

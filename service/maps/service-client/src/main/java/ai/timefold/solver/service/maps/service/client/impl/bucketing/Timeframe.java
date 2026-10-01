package ai.timefold.solver.service.maps.service.client.impl.bucketing;

import java.util.Objects;

/**
 * Type-safe identifier for a single bucket produced by a {@link TimeframeBucketing}. Wrapping the name as a dedicated
 * type prevents arbitrary strings from sneaking into the {@code Map<Timeframe, ...>} subset keys built by the maps
 * service client. The underlying string is only unwrapped at the wire boundary, where the maps service expects an
 * option of the form {@code timeframe=morning}.
 */
public record Timeframe(String name) {

    public Timeframe {
        Objects.requireNonNull(name, "Timeframe name must not be null.");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Timeframe name must not be blank.");
        }
    }
}

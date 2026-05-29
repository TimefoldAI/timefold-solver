package ai.timefold.solver.model.maps.api.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A half-open time interval {@code [from, to)} representing the period during which a location may be involved in
 * travel. {@link #from()} is inclusive; {@link #to()} is exclusive. An interval where {@code from.equals(to)} is a
 * zero-length (empty) interval — it still carries the meaning that the location is relevant at exactly {@code from}.
 */
public record TimeInterval(OffsetDateTime from, OffsetDateTime to) {

    public TimeInterval {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from (%s) must not be after to (%s)".formatted(from, to));
        }
    }

}

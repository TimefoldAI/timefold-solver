package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.Objects;
import java.util.function.BiPredicate;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "unchecked", "rawtypes" })
public enum PickerType {

    EQUAL(Objects::equals),
    LESS_THAN((a, b) -> ((Comparable) a).compareTo(b) < 0),
    LESS_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) <= 0),
    GREATER_THAN((a, b) -> ((Comparable) a).compareTo(b) > 0),
    GREATER_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) >= 0);

    private final BiPredicate<Object, Object> matcher;

    PickerType(BiPredicate<Object, Object> matcher) {
        this.matcher = matcher;
    }

    public PickerType flip() {
        return switch (this) {
            case LESS_THAN -> GREATER_THAN;
            case LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL;
            case GREATER_THAN -> LESS_THAN;
            case GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL;
            default -> throw new IllegalStateException("The joinerType (%s) cannot be flipped."
                    .formatted(this));
        };
    }

    public boolean matches(Object left, Object right) {
        try {
            return matcher.test(left, right);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Joiner (%s) threw an exception matching left (%s) and right (%s) objects."
                            .formatted(this, left, right),
                    e);
        }
    }

}

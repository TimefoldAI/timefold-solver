package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;

@SuppressWarnings({ "rawtypes", "unchecked" })
public enum JoinerType {
    EQUAL(Objects::equals),
    LESS_THAN((a, b) -> ((Comparable) a).compareTo(b) < 0),
    LESS_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) <= 0),
    GREATER_THAN((a, b) -> ((Comparable) a).compareTo(b) > 0),
    GREATER_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) >= 0),
    CONTAINING((a, b) -> ((Collection) a).contains(b)),
    CONTAINED_IN((a, b) -> ((Collection) b).contains(a)),
    CONTAINING_ANY_OF((a, b) -> containsAny((Collection) a, (Collection) b));

    private final BiPredicate<Object, Object> matcher;

    JoinerType(BiPredicate<Object, Object> matcher) {
        this.matcher = matcher;
    }

    public JoinerType flip() {
        return switch (this) {
            case EQUAL, CONTAINING_ANY_OF -> this;
            case LESS_THAN -> GREATER_THAN;
            case LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL;
            case GREATER_THAN -> LESS_THAN;
            case GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL;
            case CONTAINING -> CONTAINED_IN;
            case CONTAINED_IN -> CONTAINING;
        };
    }

    public boolean matches(Object left, Object right) {
        try {
            return matcher.test(left, right);
        } catch (Exception e) { // For easier debugging, in the absence of pointing to a specific constraint.
            throw new IllegalStateException(
                    "Joiner (%s) threw an exception matching left (%s) and right (%s) objects."
                            .formatted(this, left, right),
                    e);
        }
    }

    private static boolean containsAny(Collection<?> leftCollection, Collection<?> rightCollection) {
        return leftCollection.stream().anyMatch(rightCollection::contains);
    }

}

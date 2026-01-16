package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;

public enum JoinerType {
    EQUAL(Objects::equals),
    LESS_THAN((a, b) -> ((Comparable) a).compareTo(b) < 0),
    LESS_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) <= 0),
    GREATER_THAN((a, b) -> ((Comparable) a).compareTo(b) > 0),
    GREATER_THAN_OR_EQUAL((a, b) -> ((Comparable) a).compareTo(b) >= 0),
    CONTAIN((a, b) -> ((Collection) a).contains(b)),
    CONTAINED_IN((a, b) -> ((Collection) b).contains(a)),
    INTERSECT((a, b) -> containAny((Collection) a, (Collection) b));

    private final BiPredicate<Object, Object> matcher;

    JoinerType(BiPredicate<Object, Object> matcher) {
        this.matcher = matcher;
    }

    public JoinerType flip() {
        return switch (this) {
            case EQUAL -> this;
            case LESS_THAN -> GREATER_THAN;
            case LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL;
            case GREATER_THAN -> LESS_THAN;
            case GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL;
            case CONTAIN -> CONTAINED_IN;
            case CONTAINED_IN -> CONTAIN;
            case INTERSECT -> this;
        };
    }

    public boolean matches(Object left, Object right) {
        try {
            return matcher.test(left, right);
        } catch (Exception e) { // For easier debugging, in the absence of pointing to a specific constraint.
            throw new IllegalStateException(
                    "Joiner (" + this + ") threw an exception matching left (" + left + ") and right (" + right + ") objects.",
                    e);
        }
    }

    private static boolean containAny(Collection<?> leftCollection, Collection<?> rightCollection) {
        return leftCollection.stream().anyMatch(rightCollection::contains);
    }

    private static boolean containAll(Collection<?> leftCollection, Collection<?> rightCollection) {
        return leftCollection.containsAll(rightCollection);
    }

    private static boolean containNone(Collection<?> leftCollection, Collection<?> rightCollection) {
        return leftCollection.stream().noneMatch(rightCollection::contains);
    }

}

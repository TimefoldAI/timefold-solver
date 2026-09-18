package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Comparator;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param successor null when the element is the last of its chain, or is in no chain at all
 * @param comparator orders the elements of a chain; unassigned elements all compare equal
 * @param key the chain an element belongs to, null when the element is assigned to none
 */
@NullMarked
public record TopologicalSorter(UnaryOperator<@Nullable Object> successor,
        Comparator<Object> comparator,
        UnaryOperator<@Nullable Object> key) {
}

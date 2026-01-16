package ai.timefold.solver.core.impl.util;

import java.util.List;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

/**
 * Allows to store the same tuple in multiple downstream {@link ElementAwareLinkedList}s.
 * Needed by {@link JoinerType#CONTAINING}, {@link JoinerType#INTERSECTING}, ...
 *
 * @param <T> the tuple type
 */
@NullMarked
public record CompositeListEntry<Key_, T>(T element, List<Pair<Key_, ListEntry<T>>> children) implements ListEntry<T> {

    @Override
    public String toString() {
        return element.toString();
    }

}

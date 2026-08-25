package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.List;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NullMarked;

/**
 * Allows to store the same tuple in multiple downstream {@link ElementAwareLinkedList}s.
 * Needed by {@link JoinerType#CONTAINING}, {@link JoinerType#CONTAINING_ANY_OF}, ...
 * Each child carries the downstream {@link Indexer} resolved at put() time,
 * so that remove() does not need to look it up again.
 *
 * @param <T> the tuple type
 */
@NullMarked
record CompositeListEntry<Key_, T>(T element, List<Triple<Key_, Indexer<T>, ListEntry<T>>> children)
        implements
            ListEntry<T> {

    @Override
    public String toString() {
        return element.toString();
    }

}

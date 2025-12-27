package ai.timefold.solver.core.impl.util;

import java.util.List;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NullMarked;

/**
 * Allows to store the same tuple in multiple downstream {@link ElementAwareLinkedList}s.
 * For example, {@link JoinerType#CONTAIN} need.
 *
 * @param <T> the tuple type
 */
// TODO turn into record?
// TODO Is this a dirty hack or acceptable design? If the latter, is the name ListEntry ok?
@NullMarked
public final class CompositeListEntry<Key_, T> implements ListEntry<T> {

    private final T element;
    private final List<Pair<Key_, ListEntry<T>>> children;

    public CompositeListEntry(T element, List<Pair<Key_, ListEntry<T>>> children) {
        this.element = element;
        this.children = children;
    }

    @Override
    public T getElement() {
        return element;
    }

    public List<Pair<Key_, ListEntry<T>>> getChildren() {
        return children;
    }

}

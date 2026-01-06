package ai.timefold.solver.core.impl.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface ListEntry<T>
        permits ElementAwareLinkedList.Entry, ElementAwareArrayList.Entry, CompositeListEntry {

    T getElement();

}

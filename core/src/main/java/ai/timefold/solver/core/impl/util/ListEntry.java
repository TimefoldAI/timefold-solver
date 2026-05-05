package ai.timefold.solver.core.impl.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface ListEntry<T extends @Nullable Object>
        permits ElementAwareLinkedList.Entry, ElementAwareArrayList.Entry, CompositeListEntry {

    T element();

}

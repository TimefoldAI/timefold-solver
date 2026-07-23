package ai.timefold.solver.core.impl.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ListEntry<T extends @Nullable Object> {

    T element();

}

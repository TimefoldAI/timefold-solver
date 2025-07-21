package ai.timefold.solver.core.impl.domain.valuerange.buildin.empty;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Special range for empty value ranges.
 */
public class EmptyValueRange<T> extends AbstractCountableValueRange<T> {

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public @Nullable T get(long index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(@Nullable T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        throw new UnsupportedOperationException();
    }
}

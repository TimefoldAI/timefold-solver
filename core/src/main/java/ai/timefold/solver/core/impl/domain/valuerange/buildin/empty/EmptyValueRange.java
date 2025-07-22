package ai.timefold.solver.core.impl.domain.valuerange.buildin.empty;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Special range for empty value ranges.
 */
public final class EmptyValueRange<T> extends AbstractCountableValueRange<T> {

    public static final EmptyValueRange<Object> INSTANCE = new EmptyValueRange<>();

    private EmptyValueRange() {
        // Intentionally empty
    }

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
        return false;
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        throw new UnsupportedOperationException();
    }
}

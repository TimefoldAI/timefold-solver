package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Same as {@link ListValueRange}, but it employs a different caching strategy and remains immutable.
 *
 * @param <T> the value type
 */
@NullMarked
public abstract class AbstractIdentityValueRange<T, Range_ extends CountableValueRange<T>>
        extends AbstractCountableValueRange<T> {

    protected final Range_ valueRange;

    public AbstractIdentityValueRange(Range_ valueRange) {
        this.valueRange = Objects.requireNonNull(valueRange);
    }

    @Override
    public long getSize() {
        return valueRange.getSize();
    }

    @Override
    public @Nullable T get(long index) {
        return valueRange.get(index);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return valueRange.createOriginalIterator();
    }

    @Override
    public boolean contains(@Nullable T value) {
        return valueRange.contains(value);
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return valueRange.createRandomIterator(workingRandom);
    }

}

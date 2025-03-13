package ai.timefold.solver.core.impl.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

abstract sealed class AbstractGroupNodeConstructor<Tuple_ extends AbstractTuple>
        implements GroupNodeConstructor<Tuple_>
        permits GroupNodeConstructorWithAccumulate, GroupNodeConstructorWithoutAccumulate {

    private final Object equalityKey;

    protected AbstractGroupNodeConstructor(Object equalityKey) {
        this.equalityKey = equalityKey;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractGroupNodeConstructor<?> that
                && Objects.equals(getClass(), that.getClass())
                && Objects.equals(equalityKey, that.equalityKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(equalityKey);
    }
}

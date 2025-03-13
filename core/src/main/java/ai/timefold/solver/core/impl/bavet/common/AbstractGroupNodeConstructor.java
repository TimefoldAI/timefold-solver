package ai.timefold.solver.core.impl.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

abstract sealed class AbstractGroupNodeConstructor<Tuple_ extends AbstractTuple>
        implements GroupNodeConstructor<Tuple_>
        permits GroupNodeConstructorWithAccumulate, GroupNodeConstructorWithoutAccumulate {

    private final Object equalityKey;

    public AbstractGroupNodeConstructor(Object equalityKey) {
        this.equalityKey = equalityKey;
    }

    @Override
    public final boolean equals(Object o) {
        if (!Objects.equals(this.getClass(), o.getClass())) {
            return false;
        }
        var that = (AbstractGroupNodeConstructor<?>) o;
        return Objects.equals(equalityKey, that.equalityKey);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(equalityKey);
    }
}

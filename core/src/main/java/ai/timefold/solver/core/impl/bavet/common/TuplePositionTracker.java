package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

public record TuplePositionTracker<Tuple_ extends AbstractTuple>(int inputStorePosition)
        implements
            ElementPositionTracker<Tuple_> {

    @Override
    public int getPosition(Tuple_ element) {
        var value = element.getStore(inputStorePosition);
        return value == null ? -1 : (int) value;
    }

    @Override
    public int setPosition(Tuple_ element, int position) {
        var oldValue = getPosition(element);
        element.setStore(inputStorePosition, position);
        return oldValue;
    }

    @Override
    public int clearPosition(Tuple_ element) {
        try {
            return element.removeStore(inputStorePosition);
        } catch (NullPointerException e) {
            return -1;
        }
    }

}

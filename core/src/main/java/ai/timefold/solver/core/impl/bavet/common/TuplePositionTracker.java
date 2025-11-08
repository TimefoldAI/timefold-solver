package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record TuplePositionTracker<Tuple_ extends AbstractTuple>(int inputStorePosition)
        implements
            ElementPositionTracker<Tuple_> {

    @Override
    public void setPosition(Tuple_ element, int position) {
        element.setStore(inputStorePosition, position);
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

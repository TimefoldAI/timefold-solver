package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record TuplePositionTracker<Tuple_ extends AbstractTuple>(int inputStorePosition)
        implements
            ElementPositionTracker<Tuple_> {

    @Override
    public void setPosition(Tuple_ element, int position) { // Avoids autoboxing on updates.
        MutableInt oldPosition = element.getStore(inputStorePosition);
        if (oldPosition == null) {
            element.setStore(inputStorePosition, new MutableInt(position));
        } else {
            oldPosition.setValue(position);
        }
    }

    @Override
    public int clearPosition(Tuple_ element) {
        MutableInt oldPosition = element.removeStore(inputStorePosition);
        return oldPosition == null ? -1 : oldPosition.intValue();
    }

}

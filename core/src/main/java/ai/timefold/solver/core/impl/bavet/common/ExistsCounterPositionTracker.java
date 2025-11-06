package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

record ExistsCounterPositionTracker<Tuple_ extends AbstractTuple>(int inputStorePosition)
        implements
            ElementPositionTracker<ExistsCounter<Tuple_>> {

    @Override
    public int getPosition(ExistsCounter<Tuple_> element) {
        var tuple = element.getTuple();
        var value = tuple.getStore(inputStorePosition);
        return value == null ? -1 : (int) value;
    }

    @Override
    public int setPosition(ExistsCounter<Tuple_> element, int position) {
        var tuple = element.getTuple();
        var oldValue = getPosition(element);
        tuple.setStore(inputStorePosition, position);
        return oldValue;
    }

    @Override
    public int clearPosition(ExistsCounter<Tuple_> element) {
        try {
            return element.getTuple().removeStore(inputStorePosition);
        } catch (NullPointerException e) {
            return -1;
        }
    }

}

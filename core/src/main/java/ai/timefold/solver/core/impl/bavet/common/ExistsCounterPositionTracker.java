package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@SuppressWarnings({ "unchecked", "rawtypes" })
@NullMarked
record ExistsCounterPositionTracker<Tuple_ extends AbstractTuple>()
        implements
            ElementPositionTracker<ExistsCounter<Tuple_>> {

    private static final ExistsCounterPositionTracker INSTANCE = new ExistsCounterPositionTracker();

    public static <Tuple_ extends AbstractTuple> ExistsCounterPositionTracker<Tuple_> instance() {
        return INSTANCE;
    }

    @Override
    public void setPosition(ExistsCounter<Tuple_> element, int position) {
        element.indexedSetPositon = position;
    }

    @Override
    public int clearPosition(ExistsCounter<Tuple_> element) {
        var oldPosition = element.indexedSetPositon;
        element.indexedSetPositon = -1;
        return oldPosition;
    }

}

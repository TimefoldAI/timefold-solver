package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.UniDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerUniMoveStream<Solution_, A>
        extends InnerMoveStream<Solution_, UniTuple<A>>, UniMoveStream<Solution_, A> {

    @Override
    UniDataset<Solution_, A> getDataset();

}

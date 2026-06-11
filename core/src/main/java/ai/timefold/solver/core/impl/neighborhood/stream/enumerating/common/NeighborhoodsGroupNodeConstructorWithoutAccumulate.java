package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class NeighborhoodsGroupNodeConstructorWithoutAccumulate<Solution_, Tuple_ extends Tuple>
        extends AbstractNeighborhoodsGroupNodeConstructor<Solution_, Tuple_> {

    NeighborhoodsGroupNodeConstructorWithoutAccumulate(Object equalityKey,
            Function<SolutionView<Solution_>, GroupNodeConstructor<Tuple_>> factory) {
        super(equalityKey, factory);
    }
}

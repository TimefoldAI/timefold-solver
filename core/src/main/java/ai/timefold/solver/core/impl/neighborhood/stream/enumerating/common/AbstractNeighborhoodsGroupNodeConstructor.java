package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractNeighborhoodsGroupNodeConstructor<Solution_, Tuple_ extends Tuple>
        implements NeighborhoodsGroupNodeConstructor<Solution_, Tuple_>
        permits NeighborhoodsGroupNodeConstructorWithAccumulate,
        NeighborhoodsGroupNodeConstructorWithoutAccumulate {

    private final Object equalityKey;
    private final Function<SolutionView<Solution_>, GroupNodeConstructor<Tuple_>> factory;

    protected AbstractNeighborhoodsGroupNodeConstructor(Object equalityKey,
            Function<SolutionView<Solution_>, GroupNodeConstructor<Tuple_>> factory) {
        this.equalityKey = Objects.requireNonNull(equalityKey);
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public <Stream_ extends BavetStream> void build(AbstractNodeBuildHelper<Stream_> buildHelper,
            Stream_ parentTupleSource, Stream_ aftStream, List<Stream_> aftStreamChildList,
            Stream_ thisStream, EnvironmentMode environmentMode, SolutionView<Solution_> view) {
        factory.apply(view).build(buildHelper, parentTupleSource, aftStream,
                aftStreamChildList, thisStream, environmentMode);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractNeighborhoodsGroupNodeConstructor<?, ?> that
                && Objects.equals(getClass(), that.getClass())
                && Objects.equals(equalityKey, that.equalityKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(equalityKey);
    }
}

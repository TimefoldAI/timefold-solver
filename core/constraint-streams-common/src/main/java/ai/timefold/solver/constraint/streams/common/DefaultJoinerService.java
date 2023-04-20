package ai.timefold.solver.constraint.streams.common;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.bi.DefaultBiJoiner;
import ai.timefold.solver.constraint.streams.common.bi.FilteringBiJoiner;
import ai.timefold.solver.constraint.streams.common.penta.DefaultPentaJoiner;
import ai.timefold.solver.constraint.streams.common.penta.FilteringPentaJoiner;
import ai.timefold.solver.constraint.streams.common.quad.DefaultQuadJoiner;
import ai.timefold.solver.constraint.streams.common.quad.FilteringQuadJoiner;
import ai.timefold.solver.constraint.streams.common.tri.DefaultTriJoiner;
import ai.timefold.solver.constraint.streams.common.tri.FilteringTriJoiner;
import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.score.stream.JoinerService;
import ai.timefold.solver.core.impl.score.stream.JoinerType;

public final class DefaultJoinerService implements JoinerService {

    @Override
    public <A, B> BiJoiner<A, B> newBiJoiner(BiPredicate<A, B> filter) {
        return new FilteringBiJoiner<>(filter);
    }

    @Override
    public <A, B, Property_> BiJoiner<A, B> newBiJoiner(Function<A, Property_> leftMapping, JoinerType joinerType,
            Function<B, Property_> rightMapping) {
        return new DefaultBiJoiner<>(leftMapping, joinerType, rightMapping);
    }

    @Override
    public <A, B, C> TriJoiner<A, B, C> newTriJoiner(TriPredicate<A, B, C> filter) {
        return new FilteringTriJoiner<>(filter);
    }

    @Override
    public <A, B, C, Property_> TriJoiner<A, B, C> newTriJoiner(BiFunction<A, B, Property_> leftMapping, JoinerType joinerType,
            Function<C, Property_> rightMapping) {
        return new DefaultTriJoiner<>(leftMapping, joinerType, rightMapping);
    }

    @Override
    public <A, B, C, D> QuadJoiner<A, B, C, D> newQuadJoiner(QuadPredicate<A, B, C, D> filter) {
        return new FilteringQuadJoiner<>(filter);
    }

    @Override
    public <A, B, C, D, Property_> QuadJoiner<A, B, C, D> newQuadJoiner(TriFunction<A, B, C, Property_> leftMapping,
            JoinerType joinerType, Function<D, Property_> rightMapping) {
        return new DefaultQuadJoiner<>(leftMapping, joinerType, rightMapping);
    }

    @Override
    public <A, B, C, D, E> PentaJoiner<A, B, C, D, E> newPentaJoiner(PentaPredicate<A, B, C, D, E> filter) {
        return new FilteringPentaJoiner<>(filter);
    }

    @Override
    public <A, B, C, D, E, Property_> PentaJoiner<A, B, C, D, E> newPentaJoiner(QuadFunction<A, B, C, D, Property_> leftMapping,
            JoinerType joinerType, Function<E, Property_> rightMapping) {
        return new DefaultPentaJoiner<>(leftMapping, joinerType, rightMapping);
    }
}

package ai.timefold.solver.core.impl.score.stream.bavet;

import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

public record BavetStaticDataFactory<Solution_>(
        BavetConstraintFactory<Solution_> constraintFactory) implements PrecomputeFactory {
    @Override
    public <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass) {
        return constraintFactory.forEachUnfilteredPrecomputed(sourceClass);
    }

    @Override
    public <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A>... joiners) {
        return constraintFactory.forEachUniquePair(this::forEachUnfiltered, sourceClass, joiners);
    }
}

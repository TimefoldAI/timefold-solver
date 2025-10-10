package ai.timefold.solver.core.impl.score.stream.bavet;

import ai.timefold.solver.core.api.score.stream.StaticDataFactory;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

public record BavetStaticDataFactory<Solution_>(
        BavetConstraintFactory<Solution_> constraintFactory) implements StaticDataFactory {
    @Override
    public <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass) {
        return constraintFactory.forEachUnfilteredStatic(sourceClass);
    }
}

package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

public interface PrecomputeFactory {
    <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass);
}

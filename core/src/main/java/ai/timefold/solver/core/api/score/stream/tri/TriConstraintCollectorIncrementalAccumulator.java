package ai.timefold.solver.core.api.score.stream.tri;

public interface TriConstraintCollectorIncrementalAccumulator<A, B, C, ResultContainer_> {

    TriConstraintCollectorAccumulatedComponent<A, B, C, ResultContainer_> accumulate(ResultContainer_ resultContainer, A a, B b,
            C c);

}

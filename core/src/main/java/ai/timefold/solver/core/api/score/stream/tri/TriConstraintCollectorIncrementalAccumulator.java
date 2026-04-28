package ai.timefold.solver.core.api.score.stream.tri;

public interface TriConstraintCollectorIncrementalAccumulator<ResultContainer_, A, B, C> {

    TriConstraintCollectorAccumulatedComponent<ResultContainer_, A, B, C> accumulate(ResultContainer_ resultContainer, A a, B b,
            C c);

}

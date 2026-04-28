package ai.timefold.solver.core.api.score.stream.quad;

public interface QuadConstraintCollectorIncrementalAccumulator<ResultContainer_, A, B, C, D> {

    QuadConstraintCollectorAccumulatedComponent<ResultContainer_, A, B, C, D> accumulate(ResultContainer_ resultContainer, A a,
            B b, C c, D d);

}

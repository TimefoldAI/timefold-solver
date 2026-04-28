package ai.timefold.solver.core.api.score.stream.quad;

public interface QuadConstraintCollectorIncrementalAccumulator<A, B, C, D, ResultContainer_> {

    QuadConstraintCollectorAccumulatedComponent<A, B, C, D, ResultContainer_> accumulate(ResultContainer_ resultContainer, A a,
            B b, C c, D d);

}

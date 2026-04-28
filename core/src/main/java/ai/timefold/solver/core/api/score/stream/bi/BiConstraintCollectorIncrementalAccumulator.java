package ai.timefold.solver.core.api.score.stream.bi;

public interface BiConstraintCollectorIncrementalAccumulator<A, B, ResultContainer_> {

    BiConstraintCollectorAccumulatedComponent<A, B, ResultContainer_> accumulate(ResultContainer_ resultContainer, A a, B b);

}

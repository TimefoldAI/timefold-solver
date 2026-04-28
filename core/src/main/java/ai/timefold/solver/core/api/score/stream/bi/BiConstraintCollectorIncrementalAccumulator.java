package ai.timefold.solver.core.api.score.stream.bi;

public interface BiConstraintCollectorIncrementalAccumulator<ResultContainer_, A, B> {

    BiConstraintCollectorAccumulatedComponent<ResultContainer_, A, B> accumulate(ResultContainer_ resultContainer, A a, B b);

}

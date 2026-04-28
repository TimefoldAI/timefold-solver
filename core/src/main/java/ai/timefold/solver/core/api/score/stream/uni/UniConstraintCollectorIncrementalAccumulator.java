package ai.timefold.solver.core.api.score.stream.uni;

public interface UniConstraintCollectorIncrementalAccumulator<A, ResultContainer_> {

    UniConstraintCollectorAccumulatedComponent<A, ResultContainer_> accumulate(ResultContainer_ resultContainer, A a);

}

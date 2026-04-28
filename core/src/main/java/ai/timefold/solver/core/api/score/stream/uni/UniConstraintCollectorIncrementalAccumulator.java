package ai.timefold.solver.core.api.score.stream.uni;

public interface UniConstraintCollectorIncrementalAccumulator<ResultContainer_, A> {

    UniConstraintCollectorAccumulatedComponent<ResultContainer_, A> accumulate(ResultContainer_ resultContainer, A a);

}

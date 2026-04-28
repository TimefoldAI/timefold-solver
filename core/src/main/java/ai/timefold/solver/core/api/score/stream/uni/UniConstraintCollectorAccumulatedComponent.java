package ai.timefold.solver.core.api.score.stream.uni;

public interface UniConstraintCollectorAccumulatedComponent<A, ResultContainer_> {

    boolean update(ResultContainer_ resultContainer, A a);

    void undo();

}

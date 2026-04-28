package ai.timefold.solver.core.api.score.stream.uni;

public interface UniConstraintCollectorAccumulatedComponent<ResultContainer_, A> {

    boolean update(ResultContainer_ resultContainer, A a);

    void undo();

}

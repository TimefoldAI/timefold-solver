package ai.timefold.solver.core.api.score.stream.bi;

public interface BiConstraintCollectorAccumulatedComponent<ResultContainer_, A, B> {

    boolean update(ResultContainer_ resultContainer, A a, B b);

    void undo();

}

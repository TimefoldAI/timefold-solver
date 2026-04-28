package ai.timefold.solver.core.api.score.stream.bi;

public interface BiConstraintCollectorAccumulatedComponent<A, B, ResultContainer_> {

    boolean update(ResultContainer_ resultContainer, A a, B b);

    void undo();

}

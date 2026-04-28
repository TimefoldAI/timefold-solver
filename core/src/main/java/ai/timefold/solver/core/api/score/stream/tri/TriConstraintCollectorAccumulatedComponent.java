package ai.timefold.solver.core.api.score.stream.tri;

public interface TriConstraintCollectorAccumulatedComponent<ResultContainer_, A, B, C> {

    boolean update(ResultContainer_ resultContainer, A a, B b, C c);

    void undo();

}

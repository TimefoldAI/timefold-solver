package ai.timefold.solver.core.api.score.stream.quad;

public interface QuadConstraintCollectorAccumulatedComponent<ResultContainer_, A, B, C, D> {

    boolean update(ResultContainer_ resultContainer, A a, B b, C c, D d);

    void undo();

}

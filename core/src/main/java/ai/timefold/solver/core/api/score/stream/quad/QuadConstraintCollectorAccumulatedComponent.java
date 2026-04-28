package ai.timefold.solver.core.api.score.stream.quad;

public interface QuadConstraintCollectorAccumulatedComponent<A, B, C, D, ResultContainer_> {

    boolean update(ResultContainer_ resultContainer, A a, B b, C c, D d);

    void undo();

}

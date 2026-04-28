package ai.timefold.solver.core.api.score.stream.tri;

public interface TriConstraintCollectorAccumulatedComponent<A, B, C, ResultContainer_> {

    boolean update(ResultContainer_ resultContainer, A a, B b, C c);

    void undo();

}

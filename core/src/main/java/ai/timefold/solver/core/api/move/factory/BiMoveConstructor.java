package ai.timefold.solver.core.api.move.factory;

@FunctionalInterface
public interface BiMoveConstructor<Solution_, A, B>
        extends MoveConstructor<Solution_> {

    Move<Solution_, ?> apply(A a, B b);

}

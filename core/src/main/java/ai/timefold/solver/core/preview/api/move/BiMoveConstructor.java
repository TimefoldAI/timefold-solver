package ai.timefold.solver.core.preview.api.move;

@FunctionalInterface
public non-sealed interface BiMoveConstructor<Solution_, A, B>
        extends MoveConstructor<Solution_> {

    Move<Solution_> apply(Solution_ solution, A a, B b);

}

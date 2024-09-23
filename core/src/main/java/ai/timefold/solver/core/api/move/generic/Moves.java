package ai.timefold.solver.core.api.move.generic;

import ai.timefold.solver.core.api.move.factory.BiMoveConstructor;
import ai.timefold.solver.core.api.domain.metamodel.SolutionMetaModel;

public final class Moves {

    public static <Solution_, A, Value_> BiMoveConstructor<Solution_, A, Value_>
            change(SolutionMetaModel<Solution_> solutionMetaModel) {
        // The listeners will be adapted to use the metamodel, which will be public API.
        // The descriptor will then be an instance of variable metamodel.
        // The heavy lifting of variable lookup will happen here, so that the move is lightweight.
        return (entity, value) -> new ChangeMove<>(null, entity, value);
    }

    private Moves() {
        // No external instances.
    }

}

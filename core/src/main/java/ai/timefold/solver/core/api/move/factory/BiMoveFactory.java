package ai.timefold.solver.core.api.move.factory;

import ai.timefold.solver.core.api.domain.metamodel.SolutionMetaModel;

@FunctionalInterface
public interface BiMoveFactory<Solution_, A, B>
        extends MoveFactory<Solution_> {

    BiMoveConstructor<Solution_, A, B> createMoveSupplier(SolutionMetaModel<Solution_> solutionMetaModel);

}

package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.impl.neighborhood.DefaultNeighborhoodEvaluator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodEvaluator<Solution_> {

    static <Solution_> NeighborhoodEvaluator<Solution_> build(MoveProvider<Solution_> moveProvider,
            Class<Solution_> solutionClass, Class<?>... entityClasses) {
        return build(moveProvider, PlanningSolutionMetaModel.of(solutionClass, entityClasses));
    }

    static <Solution_> NeighborhoodEvaluator<Solution_> build(MoveProvider<Solution_> moveProvider,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultNeighborhoodEvaluator<>(moveProvider, solutionMetaModel);
    }

    EvaluatedNeighborhood<Solution_> evaluate(Solution_ solution);

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

}

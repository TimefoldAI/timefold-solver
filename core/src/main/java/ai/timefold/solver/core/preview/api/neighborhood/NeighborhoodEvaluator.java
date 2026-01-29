package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.impl.neighborhood.DefaultNeighborhoodEvaluator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodEvaluator<Solution_> {

    static <Solution_> NeighborhoodEvaluator<Solution_> build(Class<MoveProvider<Solution_>> moveProviderClass,
            Class<Solution_> solutionClass, Class<?>... entityClasses) {
        return build(moveProviderClass, PlanningSolutionMetaModel.of(solutionClass, entityClasses));
    }

    static <Solution_> NeighborhoodEvaluator<Solution_> build(Class<MoveProvider<Solution_>> moveProviderClass,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultNeighborhoodEvaluator<>(moveProviderClass, solutionMetaModel);
    }

    EvaluatedNeighborhood<Solution_> evaluate(Solution_ solution);

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

}

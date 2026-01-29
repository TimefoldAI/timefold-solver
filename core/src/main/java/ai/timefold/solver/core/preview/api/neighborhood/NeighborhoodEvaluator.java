package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodEvaluator<Solution_> {

    static <Solution_> NeighborhoodEvaluator<Solution_> build(Class<Solution_> solutionClass, Class<?>... entityClasses) {
        return null;
    }

    static <Solution_> NeighborhoodEvaluator<Solution_> build(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return null;
    }

    NeighborhoodEvaluationContext<Solution_> using(Solution_ solution);

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

}

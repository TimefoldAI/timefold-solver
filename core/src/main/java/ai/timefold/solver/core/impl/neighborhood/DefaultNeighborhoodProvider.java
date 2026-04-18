
package ai.timefold.solver.core.impl.neighborhood;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.AssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListAssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListSwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListUnassignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.UnassignMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_>
 */
@NullMarked
public final class DefaultNeighborhoodProvider<Solution_> implements NeighborhoodProvider<Solution_> {

    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<Solution_> builder) {
        var solutionMetaModel = builder.getSolutionMetaModel();
        for (var entityMetaModel : solutionMetaModel.genuineEntities()) {
            var hasBasicVariable = false;
            for (var variableMetaModel : entityMetaModel.genuineVariables()) {
                if (variableMetaModel instanceof PlanningListVariableMetaModel<Solution_, ?, ?> listVariableMetaModel) {
                    // TODO Implement 2-opt and 3-opt moves for list variables.
                    builder.add(new ListChangeMoveProvider<>(listVariableMetaModel));
                    builder.add(new ListSwapMoveProvider<>(listVariableMetaModel));
                    if (listVariableMetaModel.allowsUnassignedValues()) {
                        builder.add(new ListAssignMoveProvider<>(listVariableMetaModel));
                        builder.add(new ListUnassignMoveProvider<>(listVariableMetaModel));
                    }
                } else if (variableMetaModel instanceof PlanningVariableMetaModel<Solution_, ?, ?> basicVariableMetaModel) {
                    hasBasicVariable = true;
                    builder.add(new ChangeMoveProvider<>(basicVariableMetaModel));
                    if (basicVariableMetaModel.allowsUnassigned()) {
                        builder.add(new AssignMoveProvider<>(basicVariableMetaModel));
                        builder.add(new UnassignMoveProvider<>(basicVariableMetaModel));
                    }
                }
            }
            if (hasBasicVariable) {
                builder.add(new SwapMoveProvider<>(entityMetaModel));
            }
        }
        return builder.build();
    }

}

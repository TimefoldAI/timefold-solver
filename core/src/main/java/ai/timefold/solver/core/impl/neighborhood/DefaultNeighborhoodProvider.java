
package ai.timefold.solver.core.impl.neighborhood;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.AssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListAssignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListSwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListTailSwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.ListUnassignMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.TwoOptListMoveProvider;
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
                    // ListChangeMoveProvider's crossingNull=false:
                    // ListAssignMoveProvider/ListUnassignMoveProvider below already cover those moves,
                    // at a much higher rate than ListChangeMoveProvider's flag would.
                    builder.add(new ListChangeMoveProvider<>(listVariableMetaModel, false));
                    builder.add(new ListSwapMoveProvider<>(listVariableMetaModel));
                    // TwoOptListMoveProvider's crossingEntity=false:
                    // ListTailSwapMoveProvider below already covers the cross-entity tail swap,
                    // at a much higher rate than TwoOptListMoveProvider's flag would.
                    builder.add(new TwoOptListMoveProvider<>(listVariableMetaModel, false));
                    builder.add(new ListTailSwapMoveProvider<>(listVariableMetaModel));
                    if (listVariableMetaModel.allowsUnassignedValues()) {
                        builder.add(new ListAssignMoveProvider<>(listVariableMetaModel));
                        builder.add(new ListUnassignMoveProvider<>(listVariableMetaModel));
                    }
                } else if (variableMetaModel instanceof PlanningVariableMetaModel<Solution_, ?, ?> basicVariableMetaModel) {
                    hasBasicVariable = true;
                    // ChangeMoveProvider's crossingNull=false:
                    // AssignMoveProvider/UnassignMoveProvider below already cover those moves,
                    // at a much higher rate than ChangeMoveProvider's flag would.
                    builder.add(new ChangeMoveProvider<>(basicVariableMetaModel, false));
                    if (basicVariableMetaModel.allowsUnassigned()) {
                        builder.add(new AssignMoveProvider<>(basicVariableMetaModel));
                        builder.add(new UnassignMoveProvider<>(basicVariableMetaModel));
                    }
                }
            }
            // Swap move is the only move which switches all variables of an entity,
            // and not just one variable.
            // It only needs to be included once per entity.
            if (hasBasicVariable) {
                builder.add(new SwapMoveProvider<>(entityMetaModel));
            }
        }
        return builder.build();
    }

}

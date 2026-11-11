
package ai.timefold.solver.core.impl.neighborhood;

import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
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
        var simpleNameCountMap = solutionMetaModel.genuineEntities().stream()
                .collect(Collectors.groupingBy(entity -> entity.type().getSimpleName(), Collectors.counting()));
        for (var entityMetaModel : solutionMetaModel.genuineEntities()) {
            var entityClass = entityMetaModel.type();
            // Default IDs contain the simple name of the entity class; add the full name where that is ambiguous.
            Consumer<MoveProvider<Solution_>> add = simpleNameCountMap.get(entityClass.getSimpleName()) > 1
                    ? provider -> builder.add(provider.getId() + "@" + entityClass.getName(), provider)
                    : builder::add;
            var hasBasicVariable = false;
            for (var variableMetaModel : entityMetaModel.genuineVariables()) {
                if (variableMetaModel instanceof PlanningListVariableMetaModel<Solution_, ?, ?> listVariableMetaModel) {
                    // ListChangeMoveProvider's crossingNull=false:
                    // ListAssignMoveProvider/ListUnassignMoveProvider below already cover those moves,
                    // at a much higher rate than ListChangeMoveProvider's flag would.
                    add.accept(new ListChangeMoveProvider<>(listVariableMetaModel, false));
                    add.accept(new ListSwapMoveProvider<>(listVariableMetaModel));
                    // TwoOptListMoveProvider's crossingEntity=false:
                    // ListTailSwapMoveProvider below already covers the cross-entity tail swap,
                    // at a much higher rate than TwoOptListMoveProvider's flag would.
                    add.accept(new TwoOptListMoveProvider<>(listVariableMetaModel, false));
                    add.accept(new ListTailSwapMoveProvider<>(listVariableMetaModel));
                    if (listVariableMetaModel.allowsUnassignedValues()) {
                        add.accept(new ListAssignMoveProvider<>(listVariableMetaModel));
                        add.accept(new ListUnassignMoveProvider<>(listVariableMetaModel));
                    }
                } else if (variableMetaModel instanceof PlanningVariableMetaModel<Solution_, ?, ?> basicVariableMetaModel) {
                    hasBasicVariable = true;
                    // ChangeMoveProvider's crossingNull=false:
                    // AssignMoveProvider/UnassignMoveProvider below already cover those moves,
                    // at a much higher rate than ChangeMoveProvider's flag would.
                    add.accept(new ChangeMoveProvider<>(basicVariableMetaModel, false));
                    if (basicVariableMetaModel.allowsUnassigned()) {
                        add.accept(new AssignMoveProvider<>(basicVariableMetaModel));
                        add.accept(new UnassignMoveProvider<>(basicVariableMetaModel));
                    }
                }
            }
            // Swap move is the only move which switches all variables of an entity,
            // and not just one variable.
            // It only needs to be included once per entity.
            if (hasBasicVariable) {
                add.accept(new SwapMoveProvider<>(entityMetaModel));
            }
        }
        return builder.build();
    }

}

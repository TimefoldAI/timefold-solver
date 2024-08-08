package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase.RuinRecreateBuilderConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class ListRuinMove<Solution_> extends AbstractMove<Solution_> {

    private final Object[] ruinedValues;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    public ListRuinMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope) {
        this.ruinedValues = ruinedValues;
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableStateSupply = listVariableStateSupply;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.entityToOriginalPositionMap = new IdentityHashMap<>();
        this.entityToNewPositionMap = new IdentityHashMap<>();
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new ListRuinUndoMove<>(ruinedValues, listVariableDescriptor,
                entityToNewPositionMap, entityToOriginalPositionMap);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        for (var value : ruinedValues) {
            var location = (LocationInList) listVariableStateSupply.getLocationInList(value);
            entityToOriginalPositionMap.computeIfAbsent(location.entity(),
                    ignored -> new TreeSet<>()).add(new RuinedLocation(value, location.index()));
        }

        for (var entry : entityToOriginalPositionMap.entrySet()) {
            var entity = entry.getKey();

            innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
            for (var position : entry.getValue().descendingSet()) {
                innerScoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue());
                listVariableDescriptor.removeElement(entity, position.index());
                innerScoreDirector.afterListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue());
            }
            innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
        }
        scoreDirector.triggerVariableListeners();

        var constructionHeuristicPhase = constructionHeuristicPhaseBuilder.setElementsToRecreate(ruinedValues)
                .build();
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        constructionHeuristicPhase.solvingStarted(solverScope);
        constructionHeuristicPhase.solve(solverScope);
        constructionHeuristicPhase.solvingEnded(solverScope);
        scoreDirector.triggerVariableListeners();

        for (var ruinedValue : ruinedValues) {
            var location = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValue);
            entityToNewPositionMap.computeIfAbsent(location.entity(), ignored -> new TreeSet<>())
                    .add(new RuinedLocation(ruinedValue, location.index()));
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public String toString() {
        return "ListRuinMove{" +
                "values=" + Arrays.toString(ruinedValues) +
                ", newLocationsByEntity=" + (!entityToNewPositionMap.isEmpty() ? entityToNewPositionMap : "?") +
                '}';
    }
}

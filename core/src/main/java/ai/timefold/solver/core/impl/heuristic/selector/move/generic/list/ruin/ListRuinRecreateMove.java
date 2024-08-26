package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

public final class ListRuinRecreateMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final List<Object> ruinedValueList;
    private final Set<Object> affectedEntitySet;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    public ListRuinRecreateMove(ListVariableStateSupply<Solution_> listVariableStateSupply,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedValueList, Set<Object> affectedEntitySet) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.ruinedValueList = ruinedValueList;
        this.affectedEntitySet = affectedEntitySet;
        this.entityToOriginalPositionMap = CollectionUtils.newIdentityHashMap(affectedEntitySet.size());
        this.entityToNewPositionMap = CollectionUtils.newIdentityHashMap(affectedEntitySet.size());
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new ListRuinRecreateUndoMove<>(this, listVariableStateSupply.getSourceVariableDescriptor(),
                entityToNewPositionMap, entityToOriginalPositionMap);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        entityToNewPositionMap.clear();
        entityToOriginalPositionMap.clear();

        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        for (var value : ruinedValueList) {
            var location = (LocationInList) listVariableStateSupply.getLocationInList(value);
            entityToOriginalPositionMap.computeIfAbsent(location.entity(),
                    ignored -> new TreeSet<>()).add(new RuinedLocation(value, location.index()));
        }

        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
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

        var constructionHeuristicPhase = constructionHeuristicPhaseBuilder.withElementsToRecreate(ruinedValueList)
                .build();
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        constructionHeuristicPhase.solvingStarted(solverScope);
        constructionHeuristicPhase.solve(solverScope);
        constructionHeuristicPhase.solvingEnded(solverScope);
        scoreDirector.triggerVariableListeners();

        for (var ruinedValue : ruinedValueList) {
            var location = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValue);
            entityToNewPositionMap.computeIfAbsent(location.entity(), ignored -> new TreeSet<>())
                    .add(new RuinedLocation(ruinedValue, location.index()));
        }
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return affectedEntitySet;
    }

    @Override
    public Collection<?> getPlanningValues() {
        return ruinedValueList;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public String toString() {
        return "ListRuinMove{" +
                "values=" + ruinedValueList +
                ", newLocationsByEntity=" + (!entityToNewPositionMap.isEmpty() ? entityToNewPositionMap : "?") +
                '}';
    }
}

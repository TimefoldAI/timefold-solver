package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.move.director.VariableChangeRecordingScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

public final class ListRuinRecreateMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final List<Object> ruinedValueList;
    private final Set<Object> affectedEntitySet;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    public ListRuinRecreateMove(ListVariableStateSupply<Solution_> listVariableStateSupply,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedValueList, Set<Object> affectedEntitySet) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.ruinedValueList = ruinedValueList;
        this.affectedEntitySet = affectedEntitySet;
        this.entityToNewPositionMap = CollectionUtils.newIdentityHashMap(affectedEntitySet.size());
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        entityToNewPositionMap.clear();
        var entityToOriginalPositionMap =
                CollectionUtils.<Object, NavigableSet<RuinedLocation>> newIdentityHashMap(affectedEntitySet.size());
        for (var valueToRuin : ruinedValueList) {
            var location = listVariableStateSupply.getLocationInList(valueToRuin)
                    .ensureAssigned();
            entityToOriginalPositionMap.computeIfAbsent(location.entity(),
                    ignored -> new TreeSet<>()).add(new RuinedLocation(valueToRuin, location.index()));
        }

        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var recordingScoreDirector = (VariableChangeRecordingScoreDirector<Solution_>) scoreDirector;
        var nonRecordingScoreDirector = recordingScoreDirector.getDelegate();
        for (var entry : entityToOriginalPositionMap.entrySet()) {
            var entity = entry.getKey();
            var originalPositionSet = entry.getValue();

            // Only record before(), so we can restore the state.
            // The after() is sent straight to the real score director.
            recordingScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                    listVariableDescriptor.getFirstUnpinnedIndex(entity),
                    listVariableDescriptor.getListSize(entity));
            for (var position : originalPositionSet.descendingSet()) {
                recordingScoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue());
                listVariableDescriptor.removeElement(entity, position.index());
                recordingScoreDirector.afterListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue());
            }
            nonRecordingScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
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

        var entityToInsertedValuesMap = CollectionUtils.<Object, List<Object>> newIdentityHashMap(0);
        for (var entity : entityToOriginalPositionMap.keySet()) {
            entityToInsertedValuesMap.put(entity, new ArrayList<>());
        }

        for (var ruinedValue : ruinedValueList) {
            var location = listVariableStateSupply.getLocationInList(ruinedValue)
                    .ensureAssigned();
            entityToNewPositionMap.computeIfAbsent(location.entity(), ignored -> new TreeSet<>())
                    .add(new RuinedLocation(ruinedValue, location.index()));
            entityToInsertedValuesMap.computeIfAbsent(location.entity(), ignored -> new ArrayList<>()).add(ruinedValue);
        }

        for (var entry : entityToInsertedValuesMap.entrySet()) {
            recordingScoreDirector.recordListAssignment(listVariableDescriptor, entry.getKey(), entry.getValue());
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
    public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        var rebasedRuinedValueList = AbstractMove.rebaseList(ruinedValueList, destinationScoreDirector);
        var rebasedAffectedEntitySet = AbstractMove.rebaseSet(affectedEntitySet, destinationScoreDirector);
        return new ListRuinRecreateMove<>(listVariableStateSupply, constructionHeuristicPhaseBuilder, solverScope,
                rebasedRuinedValueList, rebasedAffectedEntitySet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ListRuinRecreateMove<?> that))
            return false;
        return Objects.equals(listVariableStateSupply, that.listVariableStateSupply)
                && Objects.equals(ruinedValueList, that.ruinedValueList)
                && Objects.equals(affectedEntitySet, that.affectedEntitySet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listVariableStateSupply, ruinedValueList, affectedEntitySet);
    }

    @Override
    public String toString() {
        return "ListRuinMove{" +
                "values=" + ruinedValueList +
                ", newLocationsByEntity=" + (!entityToNewPositionMap.isEmpty() ? entityToNewPositionMap : "?") +
                '}';
    }
}

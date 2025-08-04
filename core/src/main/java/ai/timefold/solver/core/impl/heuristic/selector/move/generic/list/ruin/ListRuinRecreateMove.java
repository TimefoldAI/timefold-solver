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
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.move.director.VariableChangeRecordingScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

public final class ListRuinRecreateMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final List<Object> ruinedValueList;
    private final Set<Object> affectedEntitySet;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final Map<Object, NavigableSet<RuinedPosition>> entityToNewPositionMap;

    public ListRuinRecreateMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedValueList, Set<Object> affectedEntitySet) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.ruinedValueList = ruinedValueList;
        this.affectedEntitySet = affectedEntitySet;
        this.entityToNewPositionMap = CollectionUtils.newIdentityHashMap(affectedEntitySet.size());
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        entityToNewPositionMap.clear();
        var variableChangeRecordingScoreDirector = (VariableChangeRecordingScoreDirector<Solution_, ?>) scoreDirector;
        try (var listVariableStateSupply = variableChangeRecordingScoreDirector.getBacking().getSupplyManager()
                .demand(listVariableDescriptor.getStateDemand())) {
            var entityToOriginalPositionMap =
                    CollectionUtils.<Object, NavigableSet<RuinedPosition>> newIdentityHashMap(affectedEntitySet.size());
            for (var valueToRuin : ruinedValueList) {
                var position = listVariableStateSupply.getElementPosition(valueToRuin)
                        .ensureAssigned();
                entityToOriginalPositionMap.computeIfAbsent(position.entity(),
                        ignored -> new TreeSet<>()).add(new RuinedPosition(valueToRuin, position.index()));
            }

            var nonRecordingScoreDirector = variableChangeRecordingScoreDirector.getBacking();
            for (var entry : entityToOriginalPositionMap.entrySet()) {
                var entity = entry.getKey();
                var originalPositionSet = entry.getValue();

                // Only record before(), so we can restore the state.
                // The after() is sent straight to the real score director.
                variableChangeRecordingScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                        listVariableDescriptor.getFirstUnpinnedIndex(entity),
                        listVariableDescriptor.getListSize(entity));
                for (var position : originalPositionSet.descendingSet()) {
                    variableChangeRecordingScoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor,
                            position.ruinedValue());
                    listVariableDescriptor.removeElement(entity, position.index());
                    variableChangeRecordingScoreDirector.afterListVariableElementUnassigned(listVariableDescriptor,
                            position.ruinedValue());
                }
                nonRecordingScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                        listVariableDescriptor.getFirstUnpinnedIndex(entity),
                        listVariableDescriptor.getListSize(entity));
            }
            scoreDirector.triggerVariableListeners();

            var constructionHeuristicPhase =
                    (RuinRecreateConstructionHeuristicPhase<Solution_>) constructionHeuristicPhaseBuilder
                            .ensureThreadSafe(variableChangeRecordingScoreDirector.getBacking())
                            .withElementsToRuin(entityToOriginalPositionMap.keySet())
                            .withElementsToRecreate(ruinedValueList)
                            .build();

            var nestedSolverScope = new SolverScope<Solution_>(solverScope.getClock());
            nestedSolverScope.setSolver(solverScope.getSolver());
            nestedSolverScope.setScoreDirector(variableChangeRecordingScoreDirector.getBacking());
            constructionHeuristicPhase.solvingStarted(nestedSolverScope);
            constructionHeuristicPhase.solve(nestedSolverScope);
            constructionHeuristicPhase.solvingEnded(nestedSolverScope);
            scoreDirector.triggerVariableListeners();

            var entityToInsertedValuesMap = CollectionUtils.<Object, List<Object>> newIdentityHashMap(0);
            for (var entity : entityToOriginalPositionMap.keySet()) {
                entityToInsertedValuesMap.put(entity, new ArrayList<>());
            }

            for (var ruinedValue : ruinedValueList) {
                var position = listVariableStateSupply.getElementPosition(ruinedValue)
                        .ensureAssigned();
                entityToNewPositionMap.computeIfAbsent(position.entity(), ignored -> new TreeSet<>())
                        .add(new RuinedPosition(ruinedValue, position.index()));
                entityToInsertedValuesMap.computeIfAbsent(position.entity(), ignored -> new ArrayList<>()).add(ruinedValue);
            }

            var onlyRecordingChangesScoreDirector = variableChangeRecordingScoreDirector.getNonDelegating();
            for (var entry : entityToInsertedValuesMap.entrySet()) {
                if (!entityToOriginalPositionMap.containsKey(entry.getKey())) {
                    // The entity has not been evaluated while creating the entityToOriginalPositionMap,
                    // meaning it is a new destination entity without a ListVariableBeforeChangeAction
                    // to restore the original elements.
                    // We need to ensure the before action is executed in order to restore the original elements.
                    var originalElementList =
                            constructionHeuristicPhase.getMissingUpdatedElementsMap().get(entry.getKey());
                    var currentElementList = List.copyOf(listVariableDescriptor.getValue(entry.getKey()));
                    // We need to first update the entity element list before tracking changes
                    // and set it back to the one from the generated solution
                    listVariableDescriptor.getValue(entry.getKey()).clear();
                    listVariableDescriptor.getValue(entry.getKey()).addAll(originalElementList);
                    onlyRecordingChangesScoreDirector.beforeListVariableChanged(listVariableDescriptor, entry.getKey(), 0,
                            originalElementList.size());
                    listVariableDescriptor.getValue(entry.getKey()).clear();
                    listVariableDescriptor.getValue(entry.getKey()).addAll(currentElementList);
                }
                // Since the solution was generated through a nested phase,
                // all actions taken to produce the solution are not accessible.
                // Therefore, we need to replicate all the actions required to generate the solution
                // while also allowing for restoring the original state.
                for (var element : entry.getValue()) {
                    onlyRecordingChangesScoreDirector.beforeListVariableElementAssigned(listVariableDescriptor, element);
                }
                onlyRecordingChangesScoreDirector.afterListVariableChanged(listVariableDescriptor, entry.getKey(),
                        listVariableDescriptor.getFirstUnpinnedIndex(entry.getKey()),
                        listVariableDescriptor.getListSize(entry.getKey()));
                for (var element : entry.getValue()) {
                    onlyRecordingChangesScoreDirector.afterListVariableElementAssigned(listVariableDescriptor, element);
                }
            }
            variableChangeRecordingScoreDirector.getBacking().getSupplyManager()
                    .cancel(listVariableDescriptor.getStateDemand());
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
        var rebasedListVariableDescriptor = ((InnerScoreDirector<Solution_, ?>) destinationScoreDirector)
                .getSolutionDescriptor().getListVariableDescriptor();
        return new ListRuinRecreateMove<>(rebasedListVariableDescriptor, constructionHeuristicPhaseBuilder, solverScope,
                rebasedRuinedValueList, rebasedAffectedEntitySet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ListRuinRecreateMove<?> that))
            return false;
        return Objects.equals(listVariableDescriptor, that.listVariableDescriptor)
                && Objects.equals(ruinedValueList, that.ruinedValueList)
                && Objects.equals(affectedEntitySet, that.affectedEntitySet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listVariableDescriptor, ruinedValueList, affectedEntitySet);
    }

    @Override
    public String toString() {
        return "ListRuinMove{" +
                "values=" + ruinedValueList +
                ", newPositionsByEntity=" + (!entityToNewPositionMap.isEmpty() ? entityToNewPositionMap : "?") +
                '}';
    }
}

package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.move.VariableChangeRecordingScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SelectorBasedListRuinRecreateMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final List<Object> ruinedValueList;
    private final SequencedSet<Object> affectedEntitySet;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final Map<Object, NavigableSet<RuinedPosition>> entityToNewPositionMap;

    public SelectorBasedListRuinRecreateMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope, List<Object> ruinedValueList, SequencedSet<Object> affectedEntitySet) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.ruinedValueList = ruinedValueList;
        this.affectedEntitySet = affectedEntitySet;
        this.entityToNewPositionMap = new IdentityHashMap<>(affectedEntitySet.size());
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        entityToNewPositionMap.clear();
        var variableChangeRecordingScoreDirector =
                scoreDirector instanceof VariableChangeRecordingScoreDirector<Solution_, ?> recordingScoreDirector
                        ? recordingScoreDirector
                        : new VariableChangeRecordingScoreDirector<>(scoreDirector);
        var nonRecordingScoreDirector = variableChangeRecordingScoreDirector.getBacking();
        var onlyRecordingChangesScoreDirector = variableChangeRecordingScoreDirector.getNonDelegating();
        try (var listVariableStateSupply = nonRecordingScoreDirector.getSupplyManager()
                .demand(listVariableDescriptor.getStateDemand())) {
            var entityToOriginalPositionMap =
                    new IdentityHashMap<Object, NavigableSet<RuinedPosition>>(affectedEntitySet.size());
            for (var valueToRuin : ruinedValueList) {
                var position = listVariableStateSupply.getElementPosition(valueToRuin)
                        .ensureAssigned();
                entityToOriginalPositionMap.computeIfAbsent(position.entity(),
                        ignored -> new TreeSet<>()).add(new RuinedPosition(valueToRuin, position.index()));
            }

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
                            .ensureThreadSafe(nonRecordingScoreDirector)
                            .withElementsToRuin(entityToOriginalPositionMap.keySet())
                            .withElementsToRecreate(ruinedValueList)
                            .build();

            var nestedSolverScope = new SolverScope<Solution_>(solverScope.getClock());
            nestedSolverScope.setSolver(solverScope.getSolver());
            nestedSolverScope.setScoreDirector(nonRecordingScoreDirector);
            constructionHeuristicPhase.solvingStarted(nestedSolverScope);
            constructionHeuristicPhase.solve(nestedSolverScope);
            constructionHeuristicPhase.solvingEnded(nestedSolverScope);
            scoreDirector.triggerVariableListeners();

            var entityToInsertedValuesMap = new IdentityHashMap<Object, List<Object>>();
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
            nonRecordingScoreDirector.getSupplyManager()
                    .cancel(listVariableDescriptor.getStateDemand());
        }
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return affectedEntitySet;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return ruinedValueList;
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        var rebasedRuinedValueList = AbstractSelectorBasedMove.rebaseList(ruinedValueList, rebaser);
        var rebasedAffectedEntitySet = AbstractSelectorBasedMove.rebaseSet(affectedEntitySet, rebaser);
        var rebasedListVariableDescriptor = ((MoveDirector<Solution_, ?>) rebaser)
                .getScoreDirector()
                .getSolutionDescriptor()
                .getListVariableDescriptor();
        return new SelectorBasedListRuinRecreateMove<>(rebasedListVariableDescriptor, constructionHeuristicPhaseBuilder,
                solverScope, rebasedRuinedValueList, rebasedAffectedEntitySet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SelectorBasedListRuinRecreateMove<?> that))
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

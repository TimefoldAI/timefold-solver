package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListRuinMove<Solution_> extends AbstractMove<Solution_> {
    protected record RuinedLocation(Object ruinedValue, int index) implements Comparable<RuinedLocation> {

        @Override
        public int compareTo(RuinedLocation other) {
            return index - other.index;
        }
    }

    protected final Object[] ruinedValues;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;
    protected final ListVariableStateSupply<Solution_> listVariableStateSupply;
    protected final DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;
    protected final SolverScope<Solution_> solverScope;
    protected final Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap;
    protected final Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap;

    public ListRuinMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope) {
        this(ruinedValues, listVariableDescriptor, listVariableStateSupply, constructionHeuristicPhase, solverScope,
                new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    private ListRuinMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope,
            Map<Object, NavigableSet<RuinedLocation>> entityToOriginalPositionMap,
            Map<Object, NavigableSet<RuinedLocation>> entityToNewPositionMap) {
        this.ruinedValues = ruinedValues;
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableStateSupply = listVariableStateSupply;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.solverScope = solverScope;
        this.entityToOriginalPositionMap = entityToOriginalPositionMap;
        this.entityToNewPositionMap = entityToNewPositionMap;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new ListRuinMove<>(ruinedValues, listVariableDescriptor, listVariableStateSupply,
                constructionHeuristicPhase, solverScope, entityToNewPositionMap, entityToOriginalPositionMap);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        VariableDescriptorAwareScoreDirector<Solution_> innerScoreDirector =
                (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (!entityToNewPositionMap.isEmpty()) {
            Set<Object> changedEntities = Collections.newSetFromMap(new IdentityHashMap<>());
            changedEntities.addAll(entityToOriginalPositionMap.keySet());
            changedEntities.addAll(entityToNewPositionMap.keySet());

            for (var entity : changedEntities) {
                innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                        listVariableDescriptor.getFirstUnpinnedIndex(entity),
                        listVariableDescriptor.getListSize(entity));
            }

            for (var entry : entityToOriginalPositionMap.entrySet()) {
                var entity = entry.getKey();
                for (var position : entry.getValue().descendingSet()) {
                    listVariableDescriptor.removeElement(entity,
                            position.index());
                }
            }

            for (var entry : entityToNewPositionMap.entrySet()) {
                var entity = entry.getKey();
                for (var position : entry.getValue()) {
                    listVariableDescriptor.addElement(entity,
                            position.index(),
                            position.ruinedValue);
                }
            }

            for (var entity : changedEntities) {
                innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                        listVariableDescriptor.getFirstUnpinnedIndex(entity),
                        listVariableDescriptor.getListSize(entity));
            }

            scoreDirector.triggerVariableListeners();
        } else {
            for (Object value : ruinedValues) {
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
                    innerScoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue);
                    listVariableDescriptor.removeElement(entity,
                            position.index());
                    innerScoreDirector.afterListVariableElementUnassigned(listVariableDescriptor, position.ruinedValue);
                }
                innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                        listVariableDescriptor.getFirstUnpinnedIndex(entity),
                        listVariableDescriptor.getListSize(entity));
            }

            scoreDirector.triggerVariableListeners();

            constructionHeuristicPhase.solvingStarted(solverScope);
            constructionHeuristicPhase.solve(solverScope);
            scoreDirector.triggerVariableListeners();

            for (Object ruinedValue : ruinedValues) {
                var location = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValue);
                entityToNewPositionMap.computeIfAbsent(location.entity(),
                        ignored -> new TreeSet<>()).add(new RuinedLocation(ruinedValue, location.index()));
            }
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

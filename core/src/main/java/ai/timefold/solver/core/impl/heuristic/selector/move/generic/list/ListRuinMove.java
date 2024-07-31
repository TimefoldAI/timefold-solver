package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    protected final Object[] ruinedValues;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;
    protected final ListVariableStateSupply<Solution_> listVariableStateSupply;
    protected final DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;
    protected final SolverScope<Solution_> solverScope;
    protected final LocationInList[] recordedOriginalPositions;
    protected final LocationInList[] recordedNewPositions;

    public ListRuinMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope) {
        this(ruinedValues, listVariableDescriptor, listVariableStateSupply, constructionHeuristicPhase, solverScope,
                new LocationInList[ruinedValues.length], new LocationInList[ruinedValues.length]);
    }

    public ListRuinMove(Object[] ruinedValues, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase, SolverScope<Solution_> solverScope,
            LocationInList[] recordedOldPositions, LocationInList[] recordedNewPositions) {
        this.ruinedValues = ruinedValues;
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableStateSupply = listVariableStateSupply;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.solverScope = solverScope;
        this.recordedOriginalPositions = recordedOldPositions;
        this.recordedNewPositions = recordedNewPositions;
    }

    @Override
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new ListRuinMove<>(ruinedValues, listVariableDescriptor, listVariableStateSupply,
                constructionHeuristicPhase, solverScope, recordedNewPositions, recordedOriginalPositions);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        VariableDescriptorAwareScoreDirector<Solution_> innerScoreDirector =
                (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (recordedNewPositions[0] != null) {
            for (int i : getRemovalOrder(recordedOriginalPositions)) {
                var oldLocation = recordedOriginalPositions[i];
                innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, oldLocation.entity(),
                        oldLocation.index(), oldLocation.index() + 1);

                listVariableDescriptor.removeElement(oldLocation.entity(),
                        oldLocation.index());

                innerScoreDirector.afterListVariableChanged(listVariableDescriptor, oldLocation.entity(),
                        oldLocation.index(), oldLocation.index());
                scoreDirector.triggerVariableListeners();
            }

            for (int i : getInsertionOrder(recordedNewPositions)) {
                var newLocation = recordedNewPositions[i];
                var value = ruinedValues[i];

                innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, newLocation.entity(),
                        newLocation.index(), newLocation.index());

                listVariableDescriptor.addElement(newLocation.entity(),
                        newLocation.index(),
                        value);

                innerScoreDirector.afterListVariableChanged(listVariableDescriptor, newLocation.entity(),
                        newLocation.index(), newLocation.index() + 1);
                scoreDirector.triggerVariableListeners();
            }
        } else {
            for (int i = 0; i < ruinedValues.length; i++) {
                recordedOriginalPositions[i] = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValues[i]);
            }
            for (Object ruinedValue : ruinedValues) {
                var oldLocation = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValue);

                innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, oldLocation.entity(), oldLocation.index(),
                        oldLocation.index() + 1);
                innerScoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor, ruinedValue);
                listVariableDescriptor.removeElement(oldLocation.entity(), oldLocation.index());
                innerScoreDirector.afterListVariableElementUnassigned(listVariableDescriptor, ruinedValue);
                innerScoreDirector.afterListVariableChanged(listVariableDescriptor, oldLocation.entity(), oldLocation.index(),
                        oldLocation.index());
                scoreDirector.triggerVariableListeners();
            }

            constructionHeuristicPhase.solvingStarted(solverScope);
            constructionHeuristicPhase.solve(solverScope);
            scoreDirector.triggerVariableListeners();

            for (int i = 0; i < ruinedValues.length; i++) {
                recordedNewPositions[i] = (LocationInList) listVariableStateSupply.getLocationInList(ruinedValues[i]);
            }
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    private static Comparator<Integer> getOrderComparator(LocationInList[] locations) {
        var entityList = Stream.of(locations).map(LocationInList::entity).toList();
        return Comparator.<Integer, Integer> comparing(index -> entityList.indexOf(locations[index].entity()))
                .thenComparing(index -> locations[index].index());
    }

    /**
     * Sorts values by entity, then by index (descending)
     * Higher indices in the same entity are removed first, so
     * they are not affected by removal of earlier values in the list.
     */
    private List<Integer> getRemovalOrder(LocationInList[] locations) {
        return IntStream.range(0, locations.length)
                .boxed()
                .sorted(getOrderComparator(locations).reversed())
                .toList();
    }

    /**
     * Sorts values by entity, then by index (ascending)
     * Lower indices in the same entity are inserted first, so
     * later values insertions don't need to shift their indices.
     */
    private List<Integer> getInsertionOrder(LocationInList[] locations) {
        return IntStream.range(0, locations.length)
                .boxed()
                .sorted(getOrderComparator(locations))
                .toList();
    }

    @Override
    public String toString() {
        return "ListRuinMove{" +
                "values=" + Arrays.toString(ruinedValues) +
                ", recordedLocationInList=" + ((recordedNewPositions != null) ? Arrays.toString(recordedNewPositions) : "?")
                +
                '}';
    }
}

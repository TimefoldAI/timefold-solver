package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListRuinMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {
    protected final EntityIndependentValueSelector<Solution_> valueSelector;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;
    protected final DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;
    protected final SolverScope<Solution_> solverScope;
    protected final ListVariableStateSupply<Solution_> listVariableStateSupply;
    protected final long minimumRuinedCount;
    protected final long maximumRuinedCount;
    protected final Random workingRandom;

    public ListRuinMoveIterator(EntityIndependentValueSelector<Solution_> valueSelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase,
            SolverScope<Solution_> solverScope,
            ListVariableStateSupply<Solution_> listVariableStateSupply,
            long minimumRuinedCount,
            long maximumRuinedCount,
            Random workingRandom) {
        this.valueSelector = valueSelector;
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.solverScope = solverScope;
        this.listVariableStateSupply = listVariableStateSupply;
        this.minimumRuinedCount = minimumRuinedCount;
        this.maximumRuinedCount = maximumRuinedCount;
        this.workingRandom = workingRandom;
    }

    @Override
    protected ListRuinMove<Solution_> createUpcomingSelection() {
        int ruinedCount = workingRandom.nextInt((int) minimumRuinedCount, (int) maximumRuinedCount + 1);
        Object[] selectedValues = new Object[ruinedCount];
        Iterator<Object> valueSelectorIterator = valueSelector.iterator();
        Set<Object> selectedValuesSet = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < ruinedCount; i++) {
            while (true) {
                var selectedEntity = valueSelectorIterator.next();
                if (selectedValuesSet.add(selectedEntity)) {
                    selectedValues[i] = selectedEntity;
                    break;
                }
            }
        }
        return new ListRuinMove<>(selectedValues, listVariableDescriptor, listVariableStateSupply, constructionHeuristicPhase,
                solverScope);
    }
}

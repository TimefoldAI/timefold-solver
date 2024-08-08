package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Random;

import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase.RuinRecreateBuilderConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinMoveIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class ListRuinMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final long minimumRuinedCount;
    private final long maximumRuinedCount;
    private final Random workingRandom;

    public ListRuinMoveIterator(EntityIndependentValueSelector<Solution_> valueSelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope,
            ListVariableStateSupply<Solution_> listVariableStateSupply, long minimumRuinedCount, long maximumRuinedCount,
            Random workingRandom) {
        this.valueSelector = valueSelector;
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.listVariableStateSupply = listVariableStateSupply;
        this.minimumRuinedCount = minimumRuinedCount;
        this.maximumRuinedCount = maximumRuinedCount;
        this.workingRandom = workingRandom;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        return RuinMoveIterator.createUpcomingSelection(workingRandom, (int) minimumRuinedCount, (int) maximumRuinedCount,
                valueSelector.iterator(),
                selectedValues -> new ListRuinMove<>(selectedValues, listVariableDescriptor, listVariableStateSupply,
                        constructionHeuristicPhaseBuilder, solverScope));
    }

}

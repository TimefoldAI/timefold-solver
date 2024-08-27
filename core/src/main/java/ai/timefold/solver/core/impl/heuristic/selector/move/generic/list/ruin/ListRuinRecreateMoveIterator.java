package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.CollectionUtils;

final class ListRuinRecreateMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final int minimumRuinedCount;
    private final int maximumRuinedCount;
    private final Random workingRandom;

    public ListRuinRecreateMoveIterator(EntityIndependentValueSelector<Solution_> valueSelector,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope,
            ListVariableStateSupply<Solution_> listVariableStateSupply, int minimumRuinedCount, int maximumRuinedCount,
            Random workingRandom) {
        this.valueSelector = valueSelector;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.listVariableStateSupply = listVariableStateSupply;
        this.minimumRuinedCount = minimumRuinedCount;
        this.maximumRuinedCount = maximumRuinedCount;
        this.workingRandom = workingRandom;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        var valueIterator = valueSelector.iterator();
        var ruinedCount = workingRandom.nextInt(minimumRuinedCount, maximumRuinedCount + 1);
        var selectedValueList = new ArrayList<>(ruinedCount);
        var affectedEntitySet = CollectionUtils.newLinkedHashSet(ruinedCount);
        var selectedValueSet = Collections.newSetFromMap(CollectionUtils.newIdentityHashMap(ruinedCount));
        for (var i = 0; i < ruinedCount; i++) {
            var remainingAttempts = ruinedCount;
            while (true) {
                if (!valueIterator.hasNext()) {
                    // Bail out; cannot select enough unique elements.
                    return NoChangeMove.getInstance();
                }
                var selectedValue = valueIterator.next();
                if (selectedValueSet.add(selectedValue)) {
                    selectedValueList.add(selectedValue);
                    var affectedEntity = listVariableStateSupply.getInverseSingleton(selectedValue);
                    if (affectedEntity != null) {
                        affectedEntitySet.add(affectedEntity);
                    }
                    break;
                } else {
                    remainingAttempts--;
                }
                if (remainingAttempts == 0) {
                    // Bail out; cannot select enough unique elements.
                    return NoChangeMove.getInstance();
                }
            }
        }
        return new ListRuinRecreateMove<>(listVariableStateSupply, constructionHeuristicPhaseBuilder, solverScope,
                selectedValueList, affectedEntitySet);
    }

}

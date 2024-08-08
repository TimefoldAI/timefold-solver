package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase.RuinRecreateBuilderConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class RuinMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntitySelector<Solution_> entitySelector;
    private final GenuineVariableDescriptor<Solution_> variableDescriptor;
    private final RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final SolverScope<Solution_> solverScope;
    private final long minimumRuinedCount;
    private final long maximumRuinedCount;
    private final Random workingRandom;

    public RuinMoveIterator(EntitySelector<Solution_> entitySelector,
            GenuineVariableDescriptor<Solution_> variableDescriptor,
            RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            SolverScope<Solution_> solverScope,
            long minimumRuinedCount,
            long maximumRuinedCount,
            Random workingRandom) {
        this.entitySelector = entitySelector;
        this.variableDescriptor = variableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.solverScope = solverScope;
        this.minimumRuinedCount = minimumRuinedCount;
        this.maximumRuinedCount = maximumRuinedCount;
        this.workingRandom = workingRandom;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        return createUpcomingSelection(workingRandom, (int) minimumRuinedCount, (int) maximumRuinedCount,
                entitySelector.iterator(),
                selectedEntities -> new RuinMove<>(selectedEntities, variableDescriptor, constructionHeuristicPhaseBuilder,
                        solverScope));
    }

    public static <Solution_> Move<Solution_> createUpcomingSelection(Random workingRandom, int minimumRuinedCount,
            int maximumRuinedCount, Iterator<Object> elementIterator, Function<Object[], Move<Solution_>> moveConstructor) {
        var ruinedCount = workingRandom.nextInt(minimumRuinedCount, maximumRuinedCount + 1);
        var selectedElements = new Object[ruinedCount];
        var selectedElementSet = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var i = 0; i < ruinedCount; i++) {
            var remainingAttempts = ruinedCount;
            while (true) {
                if (!elementIterator.hasNext()) {
                    // Bail out; cannot select enough unique elements.
                    return NoChangeMove.getInstance();
                }
                var selectedElement = elementIterator.next();
                if (selectedElementSet.add(selectedElement)) {
                    selectedElements[i] = selectedElement;
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
        return moveConstructor.apply(selectedElements);
    }

}

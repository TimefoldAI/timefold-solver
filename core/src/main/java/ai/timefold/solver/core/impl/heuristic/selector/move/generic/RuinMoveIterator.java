package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class RuinMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {
    protected final EntitySelector<Solution_> entitySelector;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    protected final RuinRecreateConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;
    protected final SolverScope<Solution_> solverScope;
    protected final long minimumRuinedCount;
    protected final long maximumRuinedCount;
    protected final Random workingRandom;

    public RuinMoveIterator(EntitySelector<Solution_> entitySelector,
            GenuineVariableDescriptor<Solution_> variableDescriptor,
            RuinRecreateConstructionHeuristicPhase<Solution_> constructionHeuristicPhase,
            SolverScope<Solution_> solverScope,
            long minimumRuinedCount,
            long maximumRuinedCount,
            Random workingRandom) {
        this.entitySelector = entitySelector;
        this.variableDescriptor = variableDescriptor;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.solverScope = solverScope;
        this.minimumRuinedCount = minimumRuinedCount;
        this.maximumRuinedCount = maximumRuinedCount;
        this.workingRandom = workingRandom;
    }

    @Override
    protected RuinMove<Solution_> createUpcomingSelection() {
        int ruinedCount = workingRandom.nextInt((int) minimumRuinedCount, (int) maximumRuinedCount + 1);
        Object[] selectedEntities = new Object[ruinedCount];
        Iterator<Object> entitySelectorIterator = entitySelector.iterator();
        Set<Object> selectedEntitiesSet = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < ruinedCount; i++) {
            while (true) {
                var selectedEntity = entitySelectorIterator.next();
                if (selectedEntitiesSet.add(selectedEntity)) {
                    selectedEntities[i] = selectedEntity;
                    break;
                }
            }
        }
        return new RuinMove<>(selectedEntities, variableDescriptor, constructionHeuristicPhase, solverScope);
    }
}

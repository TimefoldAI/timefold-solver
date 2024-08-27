package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.apache.commons.math3.util.CombinatoricsUtils;

final class RuinRecreateMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntitySelector<Solution_> entitySelector;
    private final GenuineVariableDescriptor<Solution_> variableDescriptor;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final CountSupplier minimumSelectedCountSupplier;
    private final CountSupplier maximumSelectedCountSupplier;

    private SolverScope<Solution_> solverScope;

    public RuinRecreateMoveSelector(EntitySelector<Solution_> entitySelector,
            GenuineVariableDescriptor<Solution_> variableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            CountSupplier minimumSelectedCountSupplier, CountSupplier maximumSelectedCountSupplier) {
        super();
        this.entitySelector = entitySelector;
        this.variableDescriptor = variableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.minimumSelectedCountSupplier = minimumSelectedCountSupplier;
        this.maximumSelectedCountSupplier = maximumSelectedCountSupplier;

        phaseLifecycleSupport.addEventListener(entitySelector);
    }

    @Override
    public long getSize() {
        var totalSize = 0L;
        var entityCount = entitySelector.getSize();
        var minimumSelectedCount = minimumSelectedCountSupplier.applyAsInt(entityCount);
        var maximumSelectedCount = maximumSelectedCountSupplier.applyAsInt(entityCount);
        for (int selectedCount = minimumSelectedCount; selectedCount <= maximumSelectedCount; selectedCount++) {
            // Order is significant, and each entity can only be picked once
            totalSize += CombinatoricsUtils.factorial((int) entityCount) / CombinatoricsUtils.factorial(selectedCount);
        }
        return totalSize;
    }

    @Override
    public boolean isCountable() {
        return entitySelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return entitySelector.isNeverEnding();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.solverScope = solverScope;
        this.workingRandom = solverScope.getWorkingRandom();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        var entitySelectorSize = entitySelector.getSize();
        return new RuinRecreateMoveIterator<>(entitySelector, variableDescriptor, constructionHeuristicPhaseBuilder,
                solverScope,
                minimumSelectedCountSupplier.applyAsInt(entitySelectorSize),
                maximumSelectedCountSupplier.applyAsInt(entitySelectorSize),
                workingRandom);
    }
}

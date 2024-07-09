package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Iterator;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class RuinMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {
    protected final EntitySelector<Solution_> entitySelector;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    protected final DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;

    protected final ToLongFunction<Long> minimumSelectedCountSupplier;
    protected final ToLongFunction<Long> maximumSelectedCountSupplier;

    protected SolverScope<Solution_> solverScope;

    public RuinMoveSelector(EntitySelector<Solution_> entitySelector,
            GenuineVariableDescriptor<Solution_> variableDescriptor,
            DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase,
            ToLongFunction<Long> minimumSelectedCountSupplier,
            ToLongFunction<Long> maximumSelectedCountSupplier) {
        super();
        this.entitySelector = entitySelector;
        this.variableDescriptor = variableDescriptor;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.minimumSelectedCountSupplier = minimumSelectedCountSupplier;
        this.maximumSelectedCountSupplier = maximumSelectedCountSupplier;

        phaseLifecycleSupport.addEventListener(entitySelector);
    }

    @Override
    public long getSize() {
        long totalSize = 0;
        long entityCount = entitySelector.getSize();
        var minimumSelectedCount = minimumSelectedCountSupplier.applyAsLong(entityCount);
        var maximumSelectedCount = maximumSelectedCountSupplier.applyAsLong(entityCount);
        for (long selectedCount = minimumSelectedCount; selectedCount <= maximumSelectedCount; selectedCount++) {
            // Order is significant, and each entity can only be picked once
            totalSize += CombinatoricsUtils.factorial((int) entityCount) / CombinatoricsUtils.factorial((int) selectedCount);
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
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        this.solverScope = solverScope;
        this.workingRandom = solverScope.getWorkingRandom();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new RuinMoveIterator<>(entitySelector,
                variableDescriptor,
                constructionHeuristicPhase,
                solverScope,
                minimumSelectedCountSupplier.applyAsLong(entitySelector.getSize()),
                maximumSelectedCountSupplier.applyAsLong(entitySelector.getSize()),
                workingRandom);
    }
}

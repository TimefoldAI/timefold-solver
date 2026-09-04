package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.CountSupplier;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.AbstractGenericListMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NonNull;

final class ListRuinRecreateMoveSelector<Solution_> extends AbstractGenericListMoveSelector<Solution_> {

    private final IterableValueSelector<Solution_> valueSelector;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final CountSupplier minimumSelectedCountSupplier;
    private final CountSupplier maximumSelectedCountSupplier;

    private SolverScope<Solution_> solverScope;

    public ListRuinRecreateMoveSelector(IterableValueSelector<Solution_> valueSelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            CountSupplier minimumSelectedCountSupplier, CountSupplier maximumSelectedCountSupplier) {
        super(listVariableDescriptor);
        this.valueSelector = FilteringValueSelector.ofAssigned(valueSelector, this::getListVariableStateSupply);
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.minimumSelectedCountSupplier = minimumSelectedCountSupplier;
        this.maximumSelectedCountSupplier = maximumSelectedCountSupplier;

        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    @Override
    public long getSize() {
        var totalSize = 0L;
        var valueCount = valueSelector.getSize();
        var minimumSelectedCount = minimumSelectedCountSupplier.applyAsInt(valueCount);
        var maximumSelectedCount = maximumSelectedCountSupplier.applyAsInt(valueCount);
        for (var selectedCount = minimumSelectedCount; selectedCount <= maximumSelectedCount; selectedCount++) {
            // Order is significant, and each entity can only be picked once
            totalSize += MathUtils.factorial((int) valueCount) / MathUtils.factorial(selectedCount);
        }
        return totalSize;
    }

    @Override
    public boolean isNeverEnding() {
        return valueSelector.isNeverEnding();
    }

    @Override
    public void phaseStarted(@NonNull AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.solverScope = phaseScope.getSolverScope();
    }

    @Override
    public void phaseEnded(@NonNull AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.solverScope = null;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        var valueSelectorSize = valueSelector.getSize();
        return new ListRuinRecreateMoveIterator<>(valueSelector, constructionHeuristicPhaseBuilder,
                solverScope, listVariableStateSupply,
                minimumSelectedCountSupplier.applyAsInt(valueSelectorSize),
                maximumSelectedCountSupplier.applyAsInt(valueSelectorSize),
                workingRandom);
    }
}

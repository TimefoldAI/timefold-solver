package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractOriginalChangeIterator;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractRandomChangeIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.util.SelectionUtils;
import ai.timefold.solver.core.preview.api.move.Move;

public class ChangeMoveSelector<Solution_> extends AbstractGenericMoveSelector<Solution_> {

    protected final EntitySelector<Solution_> entitySelector;
    protected final ValueSelector<Solution_> valueSelector;
    protected final boolean randomSelection;

    public ChangeMoveSelector(EntitySelector<Solution_> entitySelector, ValueSelector<Solution_> valueSelector,
            boolean randomSelection) {
        this.entitySelector = entitySelector;
        this.valueSelector = valueSelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(entitySelector);
        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public boolean supportsPhaseAndSolverCaching() {
        return true;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        return randomSelection || entitySelector.isNeverEnding() || valueSelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        if (valueSelector instanceof IterableSelector) {
            return SelectionUtils.product(entitySelector.getSize(),
                    ((IterableSelector<Solution_, ?>) valueSelector).getSize());
        } else {
            return SelectionUtils.sumOf((Iterable<Object>) entitySelector::endingIterator, valueSelector::getSize);
        }
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        final GenuineVariableDescriptor<Solution_> variableDescriptor = valueSelector.getVariableDescriptor();
        if (!randomSelection) {
            return new AbstractOriginalChangeIterator<>(entitySelector, valueSelector) {
                @Override
                protected Move<Solution_> newChangeSelection(Object entity, Object toValue) {
                    return new SelectorBasedChangeMove<>(variableDescriptor, entity, toValue);
                }
            };
        } else {
            return new AbstractRandomChangeIterator<>(entitySelector, valueSelector) {
                @Override
                protected Move<Solution_> newChangeSelection(Object entity, Object toValue) {
                    return new SelectorBasedChangeMove<>(variableDescriptor, entity, toValue);
                }
            };
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelector + ", " + valueSelector + ")";
    }

}

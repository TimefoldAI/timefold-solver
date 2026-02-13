package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractOriginalSwapIterator;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractRandomSwapIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;

public class SwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    protected final EntitySelector<Solution_> leftEntitySelector;
    protected final EntitySelector<Solution_> rightEntitySelector;
    protected final List<GenuineVariableDescriptor<Solution_>> variableDescriptorList;
    protected final boolean randomSelection;

    public SwapMoveSelector(EntitySelector<Solution_> leftEntitySelector, EntitySelector<Solution_> rightEntitySelector,
            List<GenuineVariableDescriptor<Solution_>> variableDescriptorList, boolean randomSelection) {
        this.leftEntitySelector = leftEntitySelector;
        this.rightEntitySelector = rightEntitySelector;
        this.variableDescriptorList = variableDescriptorList;
        this.randomSelection = randomSelection;
        EntityDescriptor<Solution_> leftEntityDescriptor = leftEntitySelector.getEntityDescriptor();
        EntityDescriptor<Solution_> rightEntityDescriptor = rightEntitySelector.getEntityDescriptor();
        if (!leftEntityDescriptor.getEntityClass().equals(rightEntityDescriptor.getEntityClass())) {
            throw new IllegalStateException("The selector (" + this
                    + ") has a leftEntitySelector's entityClass (" + leftEntityDescriptor.getEntityClass()
                    + ") which is not equal to the rightEntitySelector's entityClass ("
                    + rightEntityDescriptor.getEntityClass() + ").");
        }
        if (variableDescriptorList.isEmpty()) {
            throw new IllegalStateException("The selector (" + this
                    + ")'s variableDescriptors (" + variableDescriptorList + ") is empty.");
        }
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : variableDescriptorList) {
            if (!variableDescriptor.getEntityDescriptor().getEntityClass().isAssignableFrom(
                    leftEntityDescriptor.getEntityClass())) {
                throw new IllegalStateException("The selector (" + this
                        + ") has a variableDescriptor with a entityClass ("
                        + variableDescriptor.getEntityDescriptor().getEntityClass()
                        + ") which is not equal or a superclass to the leftEntitySelector's entityClass ("
                        + leftEntityDescriptor.getEntityClass() + ").");
            }
        }
        phaseLifecycleSupport.addEventListener(leftEntitySelector);
        if (leftEntitySelector != rightEntitySelector) {
            phaseLifecycleSupport.addEventListener(rightEntitySelector);
        }
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
        return randomSelection || leftEntitySelector.isNeverEnding() || rightEntitySelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        return AbstractOriginalSwapIterator.getSize(leftEntitySelector, rightEntitySelector);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (!randomSelection) {
            return new AbstractOriginalSwapIterator<>(leftEntitySelector, rightEntitySelector) {
                @Override
                protected Move<Solution_> newSwapSelection(Object leftSubSelection, Object rightSubSelection) {
                    return new SwapMove<>(variableDescriptorList, leftSubSelection, rightSubSelection);
                }
            };
        } else {
            return new AbstractRandomSwapIterator<>(leftEntitySelector, rightEntitySelector) {
                @Override
                protected Move<Solution_> newSwapSelection(Object leftSubSelection, Object rightSubSelection) {
                    return new SwapMove<>(variableDescriptorList, leftSubSelection, rightSubSelection);
                }
            };
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + leftEntitySelector + ", " + rightEntitySelector + ")";
    }

}

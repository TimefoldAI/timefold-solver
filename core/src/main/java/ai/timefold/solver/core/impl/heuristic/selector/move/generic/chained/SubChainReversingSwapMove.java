package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainReversingChangeMove.reverseChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 * This {@link Move} is not cacheable.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubChainReversingSwapMove<Solution_> extends AbstractMove<Solution_> {

    private final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected final SubChain leftSubChain;
    protected final Object leftTrailingLastEntity;
    protected final SubChain rightSubChain;
    protected final Object rightTrailingLastEntity;

    public SubChainReversingSwapMove(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply,
            SubChain leftSubChain, SubChain rightSubChain) {
        this.variableDescriptor = variableDescriptor;
        this.leftSubChain = leftSubChain;
        leftTrailingLastEntity = inverseVariableSupply.getInverseSingleton(leftSubChain.getLastEntity());
        this.rightSubChain = rightSubChain;
        rightTrailingLastEntity = inverseVariableSupply.getInverseSingleton(rightSubChain.getLastEntity());
    }

    public SubChainReversingSwapMove(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SubChain leftSubChain, Object leftTrailingLastEntity,
            SubChain rightSubChain, Object rightTrailingLastEntity) {
        this.variableDescriptor = variableDescriptor;
        this.leftSubChain = leftSubChain;
        this.rightSubChain = rightSubChain;
        this.leftTrailingLastEntity = leftTrailingLastEntity;
        this.rightTrailingLastEntity = rightTrailingLastEntity;
    }

    public SubChain getLeftSubChain() {
        return leftSubChain;
    }

    public SubChain getRightSubChain() {
        return rightSubChain;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        // Because leftFirstEntity and rightFirstEntity are unequal, chained guarantees their values are unequal too.
        return !SubChainSwapMove.containsAnyOf(rightSubChain, leftSubChain);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var leftFirstEntity = leftSubChain.getFirstEntity();
        var leftFirstValue = variableDescriptor.getValue(leftFirstEntity);
        var leftLastEntity = leftSubChain.getLastEntity();
        var rightFirstEntity = rightSubChain.getFirstEntity();
        var rightFirstValue = variableDescriptor.getValue(rightFirstEntity);
        var rightLastEntity = rightSubChain.getLastEntity();
        var leftLastEntityValue = variableDescriptor.getValue(leftLastEntity);
        var rightLastEntityValue = variableDescriptor.getValue(rightLastEntity);
        // Change the entities
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (leftLastEntity != rightFirstValue) {
            castScoreDirector.changeVariableFacade(variableDescriptor, leftLastEntity, rightFirstValue);
        }
        if (rightLastEntity != leftFirstValue) {
            castScoreDirector.changeVariableFacade(variableDescriptor, rightLastEntity, leftFirstValue);
        }
        // Reverse the chains
        reverseChain(castScoreDirector, variableDescriptor, leftLastEntity, leftLastEntityValue, leftFirstEntity);
        reverseChain(castScoreDirector, variableDescriptor, rightLastEntity, rightLastEntityValue, rightFirstEntity);
        // Reroute the new chains
        if (leftTrailingLastEntity != null) {
            if (leftTrailingLastEntity != rightFirstEntity) {
                castScoreDirector.changeVariableFacade(variableDescriptor, leftTrailingLastEntity, rightFirstEntity);
            } else {
                castScoreDirector.changeVariableFacade(variableDescriptor, leftLastEntity, rightFirstEntity);
            }
        }
        if (rightTrailingLastEntity != null) {
            if (rightTrailingLastEntity != leftFirstEntity) {
                castScoreDirector.changeVariableFacade(variableDescriptor, rightTrailingLastEntity, leftFirstEntity);
            } else {
                castScoreDirector.changeVariableFacade(variableDescriptor, rightLastEntity, leftFirstEntity);
            }
        }
    }

    @Override
    public SubChainReversingSwapMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubChainReversingSwapMove<>(variableDescriptor,
                leftSubChain.rebase(destinationScoreDirector),
                destinationScoreDirector.lookUpWorkingObject(leftTrailingLastEntity),
                rightSubChain.rebase(destinationScoreDirector),
                destinationScoreDirector.lookUpWorkingObject(rightTrailingLastEntity));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return CollectionUtils.concat(leftSubChain.getEntityList(), rightSubChain.getEntityList());
    }

    @Override
    public Collection<?> getPlanningValues() {
        List<Object> values = new ArrayList<>(2);
        values.add(variableDescriptor.getValue(leftSubChain.getFirstEntity()));
        values.add(variableDescriptor.getValue(rightSubChain.getFirstEntity()));
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SubChainReversingSwapMove<?> other = (SubChainReversingSwapMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(leftSubChain, other.leftSubChain) &&
                Objects.equals(rightSubChain, other.rightSubChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, leftSubChain, rightSubChain);
    }

    @Override
    public String toString() {
        Object oldLeftValue = variableDescriptor.getValue(leftSubChain.getFirstEntity());
        Object oldRightValue = variableDescriptor.getValue(rightSubChain.getFirstEntity());
        return leftSubChain.toDottedString() + " {" + oldLeftValue + "} <-reversing-> "
                + rightSubChain.toDottedString() + " {" + oldRightValue + "}";
    }

}

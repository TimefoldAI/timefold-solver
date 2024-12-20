package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubChainChangeMove<Solution_> extends AbstractMove<Solution_> {

    protected final SubChain subChain;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    protected final Object toPlanningValue;

    protected final Object oldTrailingLastEntity;
    protected final Object newTrailingEntity;

    public SubChainChangeMove(SubChain subChain, GenuineVariableDescriptor<Solution_> variableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply, Object toPlanningValue) {
        this.subChain = subChain;
        this.variableDescriptor = variableDescriptor;
        this.toPlanningValue = toPlanningValue;
        oldTrailingLastEntity = inverseVariableSupply.getInverseSingleton(subChain.getLastEntity());
        newTrailingEntity = toPlanningValue == null ? null
                : inverseVariableSupply.getInverseSingleton(toPlanningValue);
    }

    public SubChainChangeMove(SubChain subChain, GenuineVariableDescriptor<Solution_> variableDescriptor,
            Object toPlanningValue, Object oldTrailingLastEntity, Object newTrailingEntity) {
        this.subChain = subChain;
        this.variableDescriptor = variableDescriptor;
        this.toPlanningValue = toPlanningValue;
        this.oldTrailingLastEntity = oldTrailingLastEntity;
        this.newTrailingEntity = newTrailingEntity;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public SubChain getSubChain() {
        return subChain;
    }

    public Object getToPlanningValue() {
        return toPlanningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        if (subChain.getEntityList().contains(toPlanningValue)) {
            return false;
        }
        Object oldFirstValue = variableDescriptor.getValue(subChain.getFirstEntity());
        return !Objects.equals(oldFirstValue, toPlanningValue);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var firstEntity = subChain.getFirstEntity();
        var lastEntity = subChain.getLastEntity();
        var oldFirstValue = variableDescriptor.getValue(firstEntity);
        // Close the old chain
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (oldTrailingLastEntity != null) {
            castScoreDirector.changeVariableFacade(variableDescriptor, oldTrailingLastEntity, oldFirstValue);
        }
        // Change the entity
        castScoreDirector.changeVariableFacade(variableDescriptor, firstEntity, toPlanningValue);
        // Reroute the new chain
        if (newTrailingEntity != null) {
            castScoreDirector.changeVariableFacade(variableDescriptor, newTrailingEntity, lastEntity);
        }
    }

    @Override
    public SubChainChangeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubChainChangeMove<>(subChain.rebase(destinationScoreDirector),
                variableDescriptor,
                destinationScoreDirector.lookUpWorkingObject(toPlanningValue),
                destinationScoreDirector.lookUpWorkingObject(oldTrailingLastEntity),
                destinationScoreDirector.lookUpWorkingObject(newTrailingEntity));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<? extends Object> getPlanningEntities() {
        return subChain.getEntityList();
    }

    @Override
    public Collection<? extends Object> getPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SubChainChangeMove<?> other = (SubChainChangeMove<?>) o;
        return Objects.equals(subChain, other.subChain) &&
                Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subChain, variableDescriptor, toPlanningValue);
    }

    @Override
    public String toString() {
        Object oldFirstValue = variableDescriptor.getValue(subChain.getFirstEntity());
        return subChain.toDottedString() + " {" + oldFirstValue + " -> " + toPlanningValue + "}";
    }

}

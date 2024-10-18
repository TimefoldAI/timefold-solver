package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChain;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubChainReversingChangeMove<Solution_> extends AbstractMove<Solution_> {

    protected final SubChain subChain;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    protected final Object toPlanningValue;

    protected final Object oldTrailingLastEntity;
    protected final Object newTrailingEntity;

    public SubChainReversingChangeMove(SubChain subChain, GenuineVariableDescriptor<Solution_> variableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply, Object toPlanningValue) {
        this.subChain = subChain;
        this.variableDescriptor = variableDescriptor;
        this.toPlanningValue = toPlanningValue;
        oldTrailingLastEntity = inverseVariableSupply.getInverseSingleton(subChain.getLastEntity());
        newTrailingEntity = toPlanningValue == null ? null
                : inverseVariableSupply.getInverseSingleton(toPlanningValue);
    }

    public SubChainReversingChangeMove(SubChain subChain, GenuineVariableDescriptor<Solution_> variableDescriptor,
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
        var unmovedReverse = toPlanningValue == oldFirstValue;
        // Close the old chain
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        if (!unmovedReverse) {
            if (oldTrailingLastEntity != null) {
                castScoreDirector.changeVariableFacade(variableDescriptor, oldTrailingLastEntity, oldFirstValue);
            }
        }
        var lastEntityValue = variableDescriptor.getValue(lastEntity);
        // Change the entity
        castScoreDirector.changeVariableFacade(variableDescriptor, lastEntity, toPlanningValue);
        // Reverse the chain
        reverseChain(castScoreDirector, variableDescriptor, lastEntity, lastEntityValue, firstEntity);
        // Reroute the new chain
        if (!unmovedReverse) {
            if (newTrailingEntity != null) {
                castScoreDirector.changeVariableFacade(variableDescriptor, newTrailingEntity, firstEntity);
            }
        } else {
            if (oldTrailingLastEntity != null) {
                castScoreDirector.changeVariableFacade(variableDescriptor, oldTrailingLastEntity, firstEntity);
            }
        }
    }

    static <Solution_> void reverseChain(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector,
            VariableDescriptor<Solution_> variableDescriptor, Object entity, Object previous, Object toEntity) {
        while (entity != toEntity) {
            var value = variableDescriptor.getValue(previous);
            scoreDirector.changeVariableFacade(variableDescriptor, previous, entity);
            entity = previous;
            previous = value;
        }
    }

    @Override
    public SubChainReversingChangeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubChainReversingChangeMove<>(subChain.rebase(destinationScoreDirector),
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
        final SubChainReversingChangeMove<?> other = (SubChainReversingChangeMove<?>) o;
        return Objects.equals(subChain, other.subChain) &&
                Objects.equals(variableDescriptor.getVariableName(), other.variableDescriptor.getVariableName()) &&
                Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subChain, variableDescriptor.getVariableName(), toPlanningValue);
    }

    @Override
    public String toString() {
        Object oldFirstValue = variableDescriptor.getValue(subChain.getFirstEntity());
        return subChain.toDottedString() + " {" + oldFirstValue + " -reversing-> " + toPlanningValue + "}";
    }

}

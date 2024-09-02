package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class MovableChainedTrailingValueFilter<Solution_> implements SelectionFilter<Solution_, Object> {

    private final GenuineVariableDescriptor<Solution_> variableDescriptor;

    public MovableChainedTrailingValueFilter(GenuineVariableDescriptor<Solution_> variableDescriptor) {
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public boolean accept(ScoreDirector<Solution_> scoreDirector, Object value) {
        if (value == null) {
            return true;
        }
        SingletonInverseVariableSupply supply = retrieveSingletonInverseVariableSupply(scoreDirector);
        Object trailingEntity = supply.getInverseSingleton(value);
        EntityDescriptor<Solution_> entityDescriptor = variableDescriptor.getEntityDescriptor();
        if (trailingEntity == null || !entityDescriptor.matchesEntity(trailingEntity)) {
            return true;
        }
        return entityDescriptor.getEffectiveMovableEntityFilter().test(scoreDirector.getWorkingSolution(),
                trailingEntity);
    }

    private SingletonInverseVariableSupply retrieveSingletonInverseVariableSupply(ScoreDirector<Solution_> scoreDirector) {
        // TODO Performance loss because the supply is retrieved for every accept
        // A SelectionFilter should be optionally made aware of lifecycle events, so it can cache the supply
        SupplyManager supplyManager = ((InnerScoreDirector<Solution_, ?>) scoreDirector).getSupplyManager();
        return supplyManager.demand(new SingletonInverseVariableDemand<>(variableDescriptor));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        MovableChainedTrailingValueFilter<?> that = (MovableChainedTrailingValueFilter<?>) other;
        return Objects.equals(variableDescriptor, that.variableDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor);
    }
}

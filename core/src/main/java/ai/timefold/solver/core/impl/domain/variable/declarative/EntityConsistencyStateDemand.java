package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EntityConsistencyStateDemand<Solution_>(
        EntityDescriptor<Solution_> entityDescriptor) implements Demand<EntityConsistencyState<Solution_>> {

    @Override
    public EntityConsistencyState<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
        return new EntityConsistencyState<>(EntityConsistencyStateDemand.this.entityDescriptor);
    }
}

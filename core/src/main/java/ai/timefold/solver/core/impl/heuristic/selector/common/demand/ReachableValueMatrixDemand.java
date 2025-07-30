package ai.timefold.solver.core.impl.heuristic.selector.common.demand;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.util.MemoizingSupply;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ReachableValueMatrixDemand<Solution_> implements Demand<MemoizingSupply<ReachableValueMatrix>> {

    private final Solution_ workingSolution;
    private final ValueRangeManager<Solution_> valueRangeManager;
    private final EntityDescriptor<Solution_> entityDescriptor;
    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;

    public ReachableValueMatrixDemand(Solution_ workingSolution, ValueRangeManager<Solution_> valueRangeManager,
            EntityDescriptor<Solution_> entityDescriptor, ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        this.workingSolution = workingSolution;
        this.valueRangeManager = valueRangeManager;
        this.entityDescriptor = entityDescriptor;
        this.valueRangeDescriptor = valueRangeDescriptor;
    }

    @Override
    public MemoizingSupply<ReachableValueMatrix> createExternalizedSupply(SupplyManager supplyManager) {
        Supplier<ReachableValueMatrix> supplier = () -> {
            var entityList = entityDescriptor.extractEntities(workingSolution);
            var allValues = valueRangeManager.getFromSolution(valueRangeDescriptor, workingSolution);
            var valuesSize = allValues.getSize();
            if (valuesSize > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "The matrix %s cannot be built for the entity %s (%s) because value range has a size (%d) which is higher than Integer.MAX_VALUE."
                                .formatted(ReachableValueMatrixDemand.class.getSimpleName(),
                                        entityDescriptor.getEntityClass().getSimpleName(),
                                        valueRangeDescriptor.getVariableDescriptor().getVariableName(), valuesSize));
            }
            // list of entities reachable for a value
            var entityMatrix = new IdentityHashMap<Object, Set<Object>>((int) valuesSize);
            // list of values reachable for a value
            var valueMatrix = new IdentityHashMap<Object, Set<Object>>((int) valuesSize);
            for (var entity : entityList) {
                var valuesIterator = allValues.createOriginalIterator();
                var range = valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
                while (valuesIterator.hasNext()) {
                    var value = valuesIterator.next();
                    if (range.contains(value)) {
                        var entitySet = entityMatrix.get(value);
                        if (entitySet == null) {
                            entitySet = new LinkedHashSet<>(entityList.size());
                            entityMatrix.put(value, entitySet);
                        }
                        entitySet.add(entity);
                        // Update the value list
                        var reachableValues = valueMatrix.get(value);
                        if (reachableValues == null) {
                            reachableValues = new LinkedHashSet<>((int) valuesSize);
                            valueMatrix.put(value, reachableValues);
                        }
                        var entityValuesIterator = range.createOriginalIterator();
                        while (entityValuesIterator.hasNext()) {
                            var entityValue = entityValuesIterator.next();
                            if (!Objects.equals(entityValue, value)) {
                                reachableValues.add(entityValue);
                            }
                        }
                    }
                }
            }
            return new ReachableValueMatrix(entityMatrix, valueMatrix);
        };
        return new MemoizingSupply<>(supplier);
    }

}

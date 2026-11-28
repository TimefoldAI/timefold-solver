package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.ArrayList;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.CompositeValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class FromEntityPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_> {

    public FromEntityPropertyValueRangeDescriptor(int ordinal, GenuineVariableDescriptor<Solution_> variableDescriptor,
            MemberAccessor memberAccessor) {
        super(ordinal, variableDescriptor, memberAccessor);
    }

    @Override
    public <T> ValueRange<T> extractAllValues(Solution_ solution) {
        var entityList = variableDescriptor.getEntityDescriptor().extractEntities(solution);
        var rangesFromEntities = new ArrayList<AbstractValueRange<T>>(entityList.size());
        // Entities can share the same value range instance, such as one list of all values.
        // Its values are already in the composite, so expanding it again would only cost hash lookups.
        var rawValueRangeSet = CollectionUtils.newIdentityHashSet(entityList.size());
        for (var entity : entityList) {
            var rawValueRange = readRawValueRange(entity, solution);
            if (!rawValueRangeSet.add(rawValueRange)) {
                continue;
            }
            rangesFromEntities.add((AbstractValueRange<T>) this.<T> processValueRange(rawValueRange, entity));
        }
        return new CompositeValueRange<>(rangesFromEntities);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        return readValueRange(entity, solution);
    }

    @Override
    public long getValueRangeSize(Solution_ solution, Object entity) {
        return countValues(readRawValueRange(entity, solution), entity);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return false;
    }
}

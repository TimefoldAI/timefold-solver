package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.CompositeCountableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class CompositeValueRangeDescriptor<Solution_> extends AbstractValueRangeDescriptor<Solution_>
        implements EntityIndependentValueRangeDescriptor<Solution_> {

    protected final List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList;
    protected boolean entityIndependent;

    public CompositeValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor, boolean addNullInValueRange,
            List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList) {
        super(variableDescriptor, addNullInValueRange);
        this.childValueRangeDescriptorList = childValueRangeDescriptorList;
        entityIndependent = true;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            if (!valueRangeDescriptor.isCountable()) {
                throw new IllegalStateException("The valueRangeDescriptor (" + this
                        + ") has a childValueRangeDescriptor (" + valueRangeDescriptor
                        + ") with countable (" + valueRangeDescriptor.isCountable() + ").");
            }
            if (!valueRangeDescriptor.isEntityIndependent()) {
                entityIndependent = false;
            }
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isEntityIndependent() {
        return entityIndependent;
    }

    @Override
    public ValueRange<?> extractValueRange(Solution_ solution, Object entity) {
        return innerExtractValueRange(solution, entity);
    }

    private <T> ValueRange<T> innerExtractValueRange(Solution_ solution, Object entity) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList.add((CountableValueRange<T>) valueRangeDescriptor.extractValueRange(solution, entity));
        }
        return doNullInValueRangeWrapping(new CompositeCountableValueRange<>(childValueRangeList));
    }

    @Override
    public ValueRange<?> extractValueRange(Solution_ solution) {
        return innerExtractValueRange(solution);
    }

    private <T> ValueRange<T> innerExtractValueRange(Solution_ solution) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            var entityIndependentValueRangeDescriptor = (EntityIndependentValueRangeDescriptor<Solution_>) valueRangeDescriptor;
            childValueRangeList.add((CountableValueRange<T>) entityIndependentValueRangeDescriptor.extractValueRange(solution));
        }
        return doNullInValueRangeWrapping(new CompositeCountableValueRange<>(childValueRangeList));
    }

    @Override
    public long extractValueRangeSize(Solution_ solution, Object entity) {
        var size = addNullInValueRange ? 1L : 0L;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            size += ((CountableValueRange) valueRangeDescriptor.extractValueRange(solution, entity)).getSize();
        }
        return size;
    }

    @Override
    public long extractValueRangeSize(Solution_ solution) {
        var size = addNullInValueRange ? 1L : 0L;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            var entityIndependentValueRangeDescriptor = (EntityIndependentValueRangeDescriptor<Solution_>) valueRangeDescriptor;
            size += ((CountableValueRange) entityIndependentValueRangeDescriptor.extractValueRange(solution)).getSize();
        }
        return size;
    }

}

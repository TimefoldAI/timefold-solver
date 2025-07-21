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
        implements IterableValueRangeDescriptor<Solution_> {

    private final boolean canExtractValueRangeFromSolution;
    protected final List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList;

    public CompositeValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor, boolean acceptNullInValueRange,
            List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList) {
        super(variableDescriptor, acceptNullInValueRange);
        this.childValueRangeDescriptorList = childValueRangeDescriptorList;
        var canExtractFromSolution = true;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            if (!valueRangeDescriptor.isCountable()) {
                throw new IllegalStateException(
                        "The valueRangeDescriptor (%s) has a childValueRangeDescriptor (%s) with countable (%s)."
                                .formatted(this, valueRangeDescriptor, valueRangeDescriptor.isCountable()));
            }
            if (!valueRangeDescriptor.canExtractValueRangeFromSolution()) {
                canExtractFromSolution = false;
            }
        }
        this.canExtractValueRangeFromSolution = canExtractFromSolution;
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return canExtractValueRangeFromSolution;
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public <Value_> ValueRange<Value_> extractValueRange(Solution_ solution, Object entity) {
        return innerExtractValueRange(solution, entity);
    }

    private <T> ValueRange<T> innerExtractValueRange(Solution_ solution, Object entity) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList.add((CountableValueRange<T>) valueRangeDescriptor.<T> extractValueRange(solution, entity));
        }
        return new CompositeCountableValueRange<>(childValueRangeList);
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution) {
        return innerExtractValueRange(solution);
    }

    private <T> ValueRange<T> innerExtractValueRange(Solution_ solution) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            var iterableValueRangeDescriptor = (IterableValueRangeDescriptor<Solution_>) valueRangeDescriptor;
            childValueRangeList
                    .add((CountableValueRange<T>) iterableValueRangeDescriptor.<T> extractValueRange(solution));
        }
        return new CompositeCountableValueRange<>(childValueRangeList);
    }

    @Override
    public long extractValueRangeSize(Solution_ solution, Object entity) {
        var size = 0L;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            size += ((CountableValueRange<Object>) valueRangeDescriptor.extractValueRange(solution, entity)).getSize();
        }
        return size;
    }

    @Override
    public long extractValueRangeSize(Solution_ solution) {
        var size = 0L;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            var iterableValueRangeDescriptor = (IterableValueRangeDescriptor<Solution_>) valueRangeDescriptor;
            size += ((CountableValueRange<Object>) iterableValueRangeDescriptor.extractValueRange(solution)).getSize();
        }
        return size;
    }

}

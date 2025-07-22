package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.CompositeCountableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class CompositeValueRangeDescriptor<Solution_> extends AbstractValueRangeDescriptor<Solution_> {

    private final boolean canExtractValueRangeFromSolution;
    private final List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList;

    public CompositeValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            List<ValueRangeDescriptor<Solution_>> childValueRangeDescriptorList) {
        super(variableDescriptor);
        this.childValueRangeDescriptorList = childValueRangeDescriptorList;
        var canExtractFromSolution = true;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            if (!valueRangeDescriptor.isCountable()) {
                throw new IllegalStateException(
                        "The valueRangeDescriptor (%s) has a childValueRangeDescriptor (%s) with countable (%s)."
                                .formatted(this, valueRangeDescriptor, valueRangeDescriptor.isCountable()));
            }
            canExtractFromSolution = canExtractFromSolution && valueRangeDescriptor.canExtractValueRangeFromSolution();
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
    public <T> ValueRange<T> extractAllValues(Solution_ solution) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList.add((CountableValueRange<T>) valueRangeDescriptor.<T> extractAllValues(solution));
        }
        return new CompositeCountableValueRange<>(childValueRangeList);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        var childValueRangeList = new ArrayList<CountableValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList
                    .add((CountableValueRange<T>) valueRangeDescriptor.<T> extractValuesFromEntity(solution, entity));
        }
        return new CompositeCountableValueRange<>(childValueRangeList);
    }

}

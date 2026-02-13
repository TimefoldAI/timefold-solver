package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.CompositeValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class CompositeValueRangeDescriptor<Solution_> extends AbstractValueRangeDescriptor<Solution_> {

    private final boolean canExtractValueRangeFromSolution;
    private final List<AbstractValueRangeDescriptor<Solution_>> childValueRangeDescriptorList;

    public CompositeValueRangeDescriptor(int ordinal, GenuineVariableDescriptor<Solution_> variableDescriptor,
            List<AbstractValueRangeDescriptor<Solution_>> childValueRangeDescriptorList) {
        super(ordinal, variableDescriptor);
        this.childValueRangeDescriptorList = childValueRangeDescriptorList;
        var canExtractFromSolution = true;
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            canExtractFromSolution = canExtractFromSolution && valueRangeDescriptor.canExtractValueRangeFromSolution();
        }
        this.canExtractValueRangeFromSolution = canExtractFromSolution;
    }

    public int getValueRangeCount() {
        return childValueRangeDescriptorList.size();
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return canExtractValueRangeFromSolution;
    }

    @Override
    public <T> ValueRange<T> extractAllValues(Solution_ solution) {
        var childValueRangeList = new ArrayList<AbstractValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList.add((AbstractValueRange<T>) valueRangeDescriptor.<T> extractAllValues(solution));
        }
        return new CompositeValueRange<>(childValueRangeList);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        var childValueRangeList = new ArrayList<AbstractValueRange<T>>(childValueRangeDescriptorList.size());
        for (var valueRangeDescriptor : childValueRangeDescriptorList) {
            childValueRangeList
                    .add((AbstractValueRange<T>) valueRangeDescriptor.<T> extractValuesFromEntity(solution, entity));
        }
        return new CompositeValueRange<>(childValueRangeList);
    }

}

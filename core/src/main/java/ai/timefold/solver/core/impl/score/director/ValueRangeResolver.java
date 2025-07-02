package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

import org.jspecify.annotations.Nullable;

public interface ValueRangeResolver<Solution_> {

    <Value_> ValueRange<Value_> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @Nullable Solution_ solution, @Nullable Object entity);

    long extractValueRangeSize(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @Nullable Solution_ solution,
            @Nullable Object entity);
}

package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;

public record ProfilingTupleLifecycle<Tuple_ extends Tuple>(
        InnerConstraintProfiler constraintProfiler,
        ConstraintNodeProfileId profileId,
        TupleLifecycle<Tuple_> delegate) implements TupleLifecycle<Tuple_> {
    public ProfilingTupleLifecycle {
        constraintProfiler.register(profileId);
    }

    @Override
    public void insert(Tuple_ tuple) {
        constraintProfiler.measure(profileId, InnerConstraintProfiler.Operation.INSERT,
                () -> delegate.insert(tuple));
    }

    @Override
    public void update(Tuple_ tuple) {
        constraintProfiler.measure(profileId, InnerConstraintProfiler.Operation.UPDATE,
                () -> delegate.update(tuple));
    }

    @Override
    public void retract(Tuple_ tuple) {
        constraintProfiler.measure(profileId, InnerConstraintProfiler.Operation.RETRACT,
                () -> delegate.retract(tuple));
    }
}

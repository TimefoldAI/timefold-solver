package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.ConstraintProfiler;

public record ProfilingTupleLifecycle<Tuple_ extends AbstractTuple>(
        ConstraintProfiler constraintProfiler,
        ConstraintNodeProfileId profileId,
        TupleLifecycle<Tuple_> delegate) implements TupleLifecycle<Tuple_> {
    public ProfilingTupleLifecycle {
        constraintProfiler.register(profileId);
    }

    @Override
    public void insert(Tuple_ tuple) {
        constraintProfiler.measure(profileId, ConstraintProfiler.Operation.INSERT,
                () -> delegate.insert(tuple));
    }

    @Override
    public void update(Tuple_ tuple) {
        constraintProfiler.measure(profileId, ConstraintProfiler.Operation.UPDATE,
                () -> delegate.update(tuple));
    }

    @Override
    public void retract(Tuple_ tuple) {
        constraintProfiler.measure(profileId, ConstraintProfiler.Operation.RETRACT,
                () -> delegate.retract(tuple));
    }
}

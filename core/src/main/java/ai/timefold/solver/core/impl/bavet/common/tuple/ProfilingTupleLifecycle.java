package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ProfilingTupleLifecycle<Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final InnerConstraintProfiler constraintProfiler;
    private final ConstraintNodeProfileId profileId;
    private final TupleLifecycle<Tuple_> delegate;

    public ProfilingTupleLifecycle(InnerConstraintProfiler constraintProfiler, ConstraintNodeProfileId profileId,
            TupleLifecycle<Tuple_> delegate) {
        constraintProfiler.register(profileId);
        this.constraintProfiler = constraintProfiler;
        this.profileId = profileId;
        this.delegate = delegate;
    }

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
        this.delegate.afterAllFactsInserted(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
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

    public ConstraintNodeProfileId profileId() {
        return profileId;
    }

    public TupleLifecycle<Tuple_> delegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof ProfilingTupleLifecycle<?> other
                && Objects.equals(constraintProfiler, other.constraintProfiler)
                && Objects.equals(profileId, other.profileId)
                && Objects.equals(delegate, other.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintProfiler, profileId, delegate);
    }

    @Override
    public String toString() {
        return "ProfilingTupleLifecycle[%s, %s, %s]"
                .formatted(constraintProfiler, profileId, delegate);
    }

}

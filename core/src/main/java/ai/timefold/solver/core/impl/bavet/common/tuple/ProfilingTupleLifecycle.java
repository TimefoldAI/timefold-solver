package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;

public final class ProfilingTupleLifecycle<Tuple_ extends Tuple> implements TupleLifecycle<Tuple_> {

    private final InnerConstraintProfiler constraintProfiler;
    private final ConstraintNodeProfileId profileId;
    private final TupleLifecycle<Tuple_> delegate;

    private boolean isActive;

    public ProfilingTupleLifecycle(InnerConstraintProfiler constraintProfiler, ConstraintNodeProfileId profileId,
            TupleLifecycle<Tuple_> delegate) {
        constraintProfiler.register(profileId);
        this.constraintProfiler = constraintProfiler;
        this.profileId = profileId;
        this.delegate = delegate;
    }

    @Override
    public void initialize(boolean upstreamCanProduceTuples) {
        this.isActive = upstreamCanProduceTuples;
        this.delegate.initialize(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive;
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

    public InnerConstraintProfiler constraintProfiler() {
        return constraintProfiler;
    }

    public ConstraintNodeProfileId profileId() {
        return profileId;
    }

    public TupleLifecycle<Tuple_> delegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (ProfilingTupleLifecycle) obj;
        return Objects.equals(this.constraintProfiler, that.constraintProfiler) &&
                Objects.equals(this.profileId, that.profileId) &&
                Objects.equals(this.delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintProfiler, profileId, delegate);
    }

    @Override
    public String toString() {
        return "ProfilingTupleLifecycle[" +
                "constraintProfiler=" + constraintProfiler + ", " +
                "profileId=" + profileId + ", " +
                "delegate=" + delegate + ']';
    }

}

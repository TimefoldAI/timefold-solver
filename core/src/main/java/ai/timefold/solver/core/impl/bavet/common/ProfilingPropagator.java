package ai.timefold.solver.core.impl.bavet.common;

public record ProfilingPropagator(InnerConstraintProfiler profiler, ConstraintNodeProfileId profileId,
        Propagator delegate) implements Propagator {
    @Override
    public void propagateRetracts() {
        profiler.measure(profileId,
                InnerConstraintProfiler.Operation.RETRACT,
                delegate::propagateRetracts);
    }

    @Override
    public void propagateUpdates() {
        profiler.measure(profileId,
                InnerConstraintProfiler.Operation.UPDATE,
                delegate::propagateUpdates);
    }

    @Override
    public void propagateInserts() {
        profiler.measure(profileId,
                InnerConstraintProfiler.Operation.INSERT,
                delegate::propagateInserts);
    }
}

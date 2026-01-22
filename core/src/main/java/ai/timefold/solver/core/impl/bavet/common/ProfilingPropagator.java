package ai.timefold.solver.core.impl.bavet.common;

public record ProfilingPropagator(ConstraintProfiler profiler, ConstraintNodeProfileId profileId,
        Propagator delegate) implements Propagator {
    @Override
    public void propagateRetracts() {
        profiler.measure(profileId,
                ConstraintProfiler.Operation.RETRACT,
                delegate::propagateRetracts);
    }

    @Override
    public void propagateUpdates() {
        profiler.measure(profileId,
                ConstraintProfiler.Operation.UPDATE,
                delegate::propagateUpdates);
    }

    @Override
    public void propagateInserts() {
        profiler.measure(profileId,
                ConstraintProfiler.Operation.INSERT,
                delegate::propagateInserts);
    }
}

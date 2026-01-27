package ai.timefold.solver.core.impl.bavet.common;

import java.util.Set;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerConstraintProfiler {

    void register(ConstraintNodeProfileId profileId);

    void registerConstraint(ConstraintRef constraintRef, Set<ConstraintNodeProfileId> profileIdSet);

    void measure(ConstraintNodeProfileId profileId, Operation operation, Runnable measurable);

    void summarize();

    enum Operation {
        RETRACT,
        UPDATE,
        INSERT
    }
}

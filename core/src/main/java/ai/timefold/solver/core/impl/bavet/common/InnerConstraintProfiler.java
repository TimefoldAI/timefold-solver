package ai.timefold.solver.core.impl.bavet.common;

import java.util.Set;

import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import ai.timefold.solver.core.impl.bavet.visual.NodeGraph;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerConstraintProfiler {

    void register(ConstraintNodeProfileId profileId);

    void registerNodeGraph(NodeGraph<?> nodeGraph);

    void registerConstraint(ConstraintRef constraintRef, Set<ConstraintNodeProfileId> profileIdSet);

    void measure(ConstraintNodeProfileId profileId, Operation operation, Runnable measurable);

    void summarize();

    enum Operation {
        RETRACT,
        UPDATE,
        INSERT
    }
}

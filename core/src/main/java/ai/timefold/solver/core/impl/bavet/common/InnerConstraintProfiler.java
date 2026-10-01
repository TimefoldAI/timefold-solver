package ai.timefold.solver.core.impl.bavet.common;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerConstraintProfiler {

    void register(ConstraintNodeProfileId profileId);

    <Solution_, Stream_ extends BavetStream> void registerNodeGraph(Solution_ solution, List<AbstractNode> nodeList,
            Set<Constraint> constraintSet, Function<AbstractNode, Stream_> nodeToStreamFunction,
            Function<Stream_, AbstractNode> streamToParentNodeFunction);

    void registerConstraint(ConstraintRef constraintRef, Set<ConstraintNodeProfileId> profileIdSet);

    void measure(ConstraintNodeProfileId profileId, Operation operation, Runnable measurable);

    void summarize();

    enum Operation {
        RETRACT,
        UPDATE,
        INSERT
    }
}

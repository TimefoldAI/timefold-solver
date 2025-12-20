package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ScoreImpacter;

import org.jspecify.annotations.NullMarked;

/**
 * Instances are provided by {@link TriImpactHandler}.
 */
@NullMarked
@FunctionalInterface
public interface TriScoreImpacter<A, B, C>
        extends ScoreImpacter<TriTuple<A, B, C>> {

}

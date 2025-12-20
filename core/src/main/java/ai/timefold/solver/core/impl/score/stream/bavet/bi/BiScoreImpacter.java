package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ScoreImpacter;

import org.jspecify.annotations.NullMarked;

/**
 * Instances are provided by {@link BiImpactHandler}.
 */
@NullMarked
@FunctionalInterface
public interface BiScoreImpacter<A, B>
        extends ScoreImpacter<BiTuple<A, B>> {

}

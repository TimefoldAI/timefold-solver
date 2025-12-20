package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ScoreImpacter;

import org.jspecify.annotations.NullMarked;

/**
 * Instances are provided by {@link UniImpactHandler}.
 */
@NullMarked
@FunctionalInterface
public interface UniScoreImpacter<A>
        extends ScoreImpacter<UniTuple<A>> {

}

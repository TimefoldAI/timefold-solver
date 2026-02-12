package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ImpactHandler;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link UniTuple}-specific version of {@link ImpactHandler}.
 * The methods (inherited from {@link ImpactHandler}) match the signature of {@link UniScoreImpacter}.
 */
@NullMarked
sealed interface UniImpactHandler<A>
        extends ImpactHandler<UniTuple<A>>
        permits UniBigDecimalImpactHandler, UniLongImpactHandler {

}

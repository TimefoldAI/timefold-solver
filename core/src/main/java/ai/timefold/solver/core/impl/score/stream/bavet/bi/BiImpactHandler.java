package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ImpactHandler;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link BiTuple}-specific version of {@link ImpactHandler}.
 * The methods (inherited from {@link ImpactHandler}) match the signature of {@link BiScoreImpacter}.
 */
@NullMarked
sealed interface BiImpactHandler<A, B>
        extends ImpactHandler<BiTuple<A, B>>
        permits BiBigDecimalImpactHandler, BiIntImpactHandler, BiLongImpactHandler {

}

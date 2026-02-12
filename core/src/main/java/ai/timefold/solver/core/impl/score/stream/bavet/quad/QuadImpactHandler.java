package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ImpactHandler;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link QuadTuple}-specific version of {@link ImpactHandler}.
 * The methods (inherited from {@link ImpactHandler}) match the signature of {@link QuadScoreImpacter}.
 */
@NullMarked
sealed interface QuadImpactHandler<A, B, C, D>
        extends ImpactHandler<QuadTuple<A, B, C, D>>
        permits QuadBigDecimalImpactHandler, QuadLongImpactHandler {

}

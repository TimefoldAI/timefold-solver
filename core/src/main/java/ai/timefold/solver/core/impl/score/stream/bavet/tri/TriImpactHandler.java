package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ImpactHandler;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link TriTuple}-specific version of {@link ImpactHandler}.
 * The methods (inherited from {@link ImpactHandler}) match the signature of {@link TriScoreImpacter}.
 */
@NullMarked
sealed interface TriImpactHandler<A, B, C>
        extends ImpactHandler<TriTuple<A, B, C>>
        permits TriBigDecimalImpactHandler, TriIntImpactHandler, TriLongImpactHandler {

}

package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface ScoreImpacter<Tuple_ extends Tuple>
        extends BiFunction<WeightedScoreImpacter<?, ?>, Tuple_, UndoScoreImpacter> {

}

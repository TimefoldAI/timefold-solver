package ai.timefold.solver.constraint.streams.common.bi;

import java.util.Collection;
import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.common.ConstraintConstructor;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;

@FunctionalInterface
public interface BiConstraintConstructor<A, B, Score_ extends Score<Score_>>
        extends ConstraintConstructor<Score_, TriFunction<A, B, Score_, Object>, BiFunction<A, B, Collection<?>>> {

}

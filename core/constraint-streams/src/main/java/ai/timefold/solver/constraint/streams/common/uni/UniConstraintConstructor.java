package ai.timefold.solver.constraint.streams.common.uni;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.ConstraintConstructor;
import ai.timefold.solver.core.api.score.Score;

@FunctionalInterface
public interface UniConstraintConstructor<A, Score_ extends Score<Score_>>
        extends ConstraintConstructor<Score_, BiFunction<A, Score_, Object>, Function<A, Collection<?>>> {

}

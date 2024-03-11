package ai.timefold.solver.core.impl.score.stream.common.uni;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintConstructor;

@FunctionalInterface
public interface UniConstraintConstructor<A, Score_ extends Score<Score_>>
        extends ConstraintConstructor<Score_, BiFunction<A, Score_, Object>, Function<A, Collection<?>>> {

}

package ai.timefold.solver.constraint.streams.common.tri;

import java.util.Collection;

import ai.timefold.solver.constraint.streams.common.ConstraintConstructor;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;

@FunctionalInterface
public interface TriConstraintConstructor<A, B, C, Score_ extends Score<Score_>>
        extends ConstraintConstructor<Score_, QuadFunction<A, B, C, Score_, Object>, TriFunction<A, B, C, Collection<?>>> {

}

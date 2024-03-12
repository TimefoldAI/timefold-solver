package ai.timefold.solver.core.impl.score.stream.common.tri;

import java.util.Collection;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintConstructor;

@FunctionalInterface
public interface TriConstraintConstructor<A, B, C, Score_ extends Score<Score_>>
        extends ConstraintConstructor<Score_, QuadFunction<A, B, C, Score_, Object>, TriFunction<A, B, C, Collection<?>>> {

}

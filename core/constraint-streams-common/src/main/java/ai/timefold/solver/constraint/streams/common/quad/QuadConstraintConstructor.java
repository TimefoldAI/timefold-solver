package ai.timefold.solver.constraint.streams.common.quad;

import java.util.Collection;

import ai.timefold.solver.constraint.streams.common.ConstraintConstructor;
import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;

@FunctionalInterface
public interface QuadConstraintConstructor<A, B, C, D, Score_ extends Score<Score_>>
        extends
        ConstraintConstructor<Score_, PentaFunction<A, B, C, D, Score_, Object>, QuadFunction<A, B, C, D, Collection<?>>> {

}

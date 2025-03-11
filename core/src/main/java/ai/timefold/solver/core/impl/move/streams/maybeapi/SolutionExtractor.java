package ai.timefold.solver.core.impl.move.streams.maybeapi;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import ai.timefold.solver.core.preview.api.move.SolutionView;

@FunctionalInterface
public interface SolutionExtractor<Solution_, A>
        extends BiFunction<SolutionView<Solution_>, Solution_, Stream<A>> {
}

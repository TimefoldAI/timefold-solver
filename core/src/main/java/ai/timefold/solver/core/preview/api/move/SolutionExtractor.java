package ai.timefold.solver.core.preview.api.move;

import java.util.function.BiFunction;
import java.util.stream.Stream;

@FunctionalInterface
public interface SolutionExtractor<Solution_, A>
        extends BiFunction<SolutionView<Solution_>, Solution_, Stream<A>> {
}

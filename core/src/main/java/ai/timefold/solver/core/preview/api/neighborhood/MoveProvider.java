package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;

import org.jspecify.annotations.NullMarked;

/**
 * Implement this to provide a definition for one move type.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see NeighborhoodTester How to test {@link MoveProvider}s.
 */
@NullMarked
public interface MoveProvider<Solution_> {

    MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory);

    /**
     * The ID under which {@link NeighborhoodBuilder#add(MoveProvider)} registers this provider.
     * IDs must be unique in a neighborhood.
     *
     * @return by default the simple name of the class;
     *         "custom" for a lambda or an anonymous class, whose names are not stable
     */
    default String getId() {
        return MoveProviderUtil.defaultId(this);
    }

}

package ai.timefold.solver.core.impl.neighborhood.maybeapi;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodProvider<Solution_> {

    Neighborhood defineNeighborhood(NeighborhoodBuilder<Solution_> builder);

}

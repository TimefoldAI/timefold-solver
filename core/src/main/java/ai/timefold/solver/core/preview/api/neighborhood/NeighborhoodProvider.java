package ai.timefold.solver.core.preview.api.neighborhood;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodProvider<Solution_> {

    Neighborhood defineNeighborhood(NeighborhoodBuilder<Solution_> builder);

}

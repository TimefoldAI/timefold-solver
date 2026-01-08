package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhood<Solution_> implements Neighborhood {

    private final List<MoveProvider<Solution_>> moveProviderList;

    public DefaultNeighborhood(List<MoveProvider<Solution_>> moveProviders) {
        this.moveProviderList = Objects.requireNonNull(moveProviders);
    }

    public List<MoveProvider<Solution_>> getMoveProviderList() {
        return moveProviderList;
    }

}

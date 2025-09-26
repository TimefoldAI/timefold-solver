package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.Neighborhood;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhood<Solution_> implements Neighborhood {

    private final List<MoveDefinition<Solution_>> moveDefinitionList;

    public DefaultNeighborhood(List<MoveDefinition<Solution_>> moveDefinitions) {
        this.moveDefinitionList = Objects.requireNonNull(moveDefinitions);
    }

    public List<MoveDefinition<Solution_>> getMoveDefinitionList() {
        return moveDefinitionList;
    }

}

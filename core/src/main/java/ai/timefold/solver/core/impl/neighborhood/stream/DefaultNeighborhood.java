package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Collections;
import java.util.Objects;
import java.util.SequencedMap;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultNeighborhood<Solution_>(SequencedMap<String, MoveProvider<Solution_>> moveProviderMap)
        implements
            Neighborhood {

    /**
     * @param moveProviderMap ID to move provider, in registration order
     */
    public DefaultNeighborhood(SequencedMap<String, MoveProvider<Solution_>> moveProviderMap) {
        this.moveProviderMap = Collections.unmodifiableSequencedMap(Objects.requireNonNull(moveProviderMap));
        if (moveProviderMap.isEmpty()) {
            throw new IllegalArgumentException("""
                    The neighborhood must have at least one move provider.
                    Maybe add a move provider via NeighborhoodBuilder.add().""");
        }
    }

}

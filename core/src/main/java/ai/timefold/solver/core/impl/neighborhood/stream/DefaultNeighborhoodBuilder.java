package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhoodBuilder<Solution_> implements NeighborhoodBuilder<Solution_> {

    private final PlanningSolutionMetaModel<Solution_> solutionMetaModel;
    private final List<Entry<Solution_>> entryList = new ArrayList<>();

    public DefaultNeighborhoodBuilder(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this.solutionMetaModel = Objects.requireNonNull(solutionMetaModel);
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return solutionMetaModel;
    }

    @Override
    public NeighborhoodBuilder<Solution_> add(String id, MoveProvider<Solution_> moveProvider) {
        if (id.isBlank()) {
            throw new IllegalArgumentException("The id (%s) of the move provider (%s) must not be blank."
                    .formatted(id, moveProvider));
        }
        entryList.add(new Entry<>(id, Objects.requireNonNull(moveProvider)));
        return this;
    }

    @Override
    public DefaultNeighborhood<Solution_> build() {
        var moveProviderMap = new LinkedHashMap<String, MoveProvider<Solution_>>(entryList.size());
        var idByProviderMap = new LinkedHashMap<MoveProvider<Solution_>, String>(entryList.size());
        for (var entry : entryList) {
            var previousId = idByProviderMap.get(entry.moveProvider());
            if (previousId != null) {
                if (previousId.equals(entry.id())) {
                    continue;
                }
                throw new IllegalStateException("""
                        The move provider (%s) is added twice, with the ids (%s) and (%s).
                        Maybe add it only once."""
                        .formatted(entry.moveProvider(), previousId, entry.id()));
            }
            var previousProvider = moveProviderMap.get(entry.id());
            if (previousProvider != null) {
                throw new IllegalStateException("""
                        The id (%s) is used by two move providers (%s) and (%s).
                        Maybe give one of them its own id with add(id, moveProvider)."""
                        .formatted(entry.id(), previousProvider, entry.moveProvider()));
            }
            moveProviderMap.put(entry.id(), entry.moveProvider());
            idByProviderMap.put(entry.moveProvider(), entry.id());
        }
        return new DefaultNeighborhood<>(moveProviderMap);
    }

    private record Entry<Solution_>(String id, MoveProvider<Solution_> moveProvider) {
    }

}

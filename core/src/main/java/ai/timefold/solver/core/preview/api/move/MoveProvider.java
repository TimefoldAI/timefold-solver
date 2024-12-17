package ai.timefold.solver.core.preview.api.move;

import java.util.function.BiFunction;

@FunctionalInterface
public interface MoveProvider<Solution_>
        extends BiFunction<DatasetFactory<Solution_>, Picker<Solution_>, MoveConstructor<Solution_>> {
}

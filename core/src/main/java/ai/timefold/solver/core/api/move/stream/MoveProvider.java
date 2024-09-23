package ai.timefold.solver.core.api.move.stream;

import java.util.function.Function;

import ai.timefold.solver.core.api.move.factory.MoveConstructor;

@FunctionalInterface
public interface MoveProvider<Solution_>
        extends Function<MoveStreams<Solution_>, MoveConstructor<Solution_>> {
}

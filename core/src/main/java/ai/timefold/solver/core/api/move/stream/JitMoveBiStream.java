package ai.timefold.solver.core.api.move.stream;

import ai.timefold.solver.core.api.move.factory.BiMoveConstructor;
import ai.timefold.solver.core.api.move.factory.BiMoveFactory;

public interface JitMoveBiStream<Solution_, A, B> extends JitMoveStream<Solution_> {

    BiMoveConstructor<Solution_, A, B> asMove(BiMoveFactory<Solution_, A, B> moveFactory);

}

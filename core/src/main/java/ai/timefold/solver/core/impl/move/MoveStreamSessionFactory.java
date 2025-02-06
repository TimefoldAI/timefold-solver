package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.impl.move.director.MoveStreamSession;

public interface MoveStreamSessionFactory<Solution_> {

    MoveStreamSession<Solution_> createMoveStreamSession(Solution_ workingSolution);

}

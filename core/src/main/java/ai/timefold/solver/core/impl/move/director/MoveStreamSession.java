package ai.timefold.solver.core.impl.move.director;

public interface MoveStreamSession<Solution_> {

    void resetWorkingSolution(Solution_ workingSolution);

    void insert(Object problemFactOrEntity);

    void update(Object problemFactOrEntity);

    void retract(Object problemFactOrEntity);

    void settle();

}

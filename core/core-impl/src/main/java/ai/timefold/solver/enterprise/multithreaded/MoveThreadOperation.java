package ai.timefold.solver.enterprise.multithreaded;

abstract class MoveThreadOperation<Solution_> {

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

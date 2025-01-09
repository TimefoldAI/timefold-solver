package ai.timefold.solver.core.impl.move.director;

/**
 * Custom change actions enable recording actions in various ways. There are situations where the existing score director
 * events cannot meet the requirements for recording actions, such as nested phases.
 *
 * @see ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMove
 */
@FunctionalInterface
public interface CustomChangeAction<Solution_> {

    void apply(ActionRecorder<Solution_> recorder);
}

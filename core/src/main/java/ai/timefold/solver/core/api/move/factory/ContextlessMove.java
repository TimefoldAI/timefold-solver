package ai.timefold.solver.core.api.move.factory;

import java.util.Collection;

/**
 * A simplified version of {@link Move} which requires no context.
 * The user will implement {@link #run(MoveDirector)} instead of {@link #run(MoveDirector, Void)}.
 *
 * @param <Solution_>
 */
public interface ContextlessMove<Solution_> extends Move<Solution_, Void> {

    @Override
    default Void prepareContext() {
        return null;
    }

    @Override
    default void run(MoveDirector<Solution_> moveDirector, Void unused) {
        run(moveDirector);
    }

    void run(MoveDirector<Solution_> moveDirector);

    @Override
    default ContextlessMove<Solution_> rebase(Rebaser rebaser, Void unused) {
        return rebase(rebaser);
    }

    ContextlessMove<Solution_> rebase(Rebaser rebaser);

    @Override
    default Collection<?> getPlanningEntities(Void unused) {
        return getPlanningEntities();
    }

    Collection<?> getPlanningEntities();

    @Override
    default Collection<?> getPlanningValues(Void unused) {
        return getPlanningValues();
    }

    Collection<?> getPlanningValues();

    @Override
    default String toString(Void unused) {
        return toString();
    }

}

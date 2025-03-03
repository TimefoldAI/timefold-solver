package ai.timefold.solver.core.impl.solver.termination;

import org.jspecify.annotations.NullMarked;

/**
 * Only exists to make testing easier.
 * This type is not accessible outside of this package.
 * 
 * @param <Solution_>
 */
@NullMarked
non-sealed interface MockablePhaseTermination<Solution_> extends PhaseTermination<Solution_> {

}

package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;

public interface RandomSource {
    /**
     * Creates a new {@link RandomSource} from a given seed.
     *
     * @param seed Controls the random sequence generated.
     *
     * @return A new {@link RandomSource} from a given seed.
     */
    static RandomSource seeded(long seed) {
        return DefaultRandomSource.seeded(seed);
    }

    /**
     * Used by {@link ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector}
     * and other components.
     *
     * @return A {@link RandomGenerator} instance that advances the source
     *         {@link RandomGenerator} state.
     */
    RandomGenerator moveUsage();

    /**
     * Used by {@link ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor}
     * and other components inbetween move generation.
     *
     * @return A {@link RandomGenerator} instance that does not advance the source
     *         {@link RandomGenerator} state (and hence, any usage does not affect
     *         reproducibility).
     */
    RandomGenerator acceptorUsage();
}

package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.solver.AbstractSolver;

/**
 * Provide access to a {@link RandomGenerator}. Random state
 * is reset each step in {@link AbstractSolver#stepStarted} and
 * {@link AbstractSolver#stepEnded}. As such, it does not matter
 * how many random calls is performed in a step, provided usage
 * of the {@link RandomGenerator} is fully deterministic.
 * <br/>
 * Moves and acceptors use different {@link RandomGenerator} since
 * an acceptor is called when a move has been evaluated, and
 * in a multithreaded context, we can generate moves while waiting
 * for a prior move to be evaluated. Thus, if they used the same
 * {@link RandomGenerator}, the moves generated would not be deterministic,
 * since sometimes there will be another random call since an earlier move was
 * evaluated, and sometimes there won't be.
 */
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
     * Used by {@link MoveRepository} and other components for generating moves.
     *
     * @return A {@link RandomGenerator} exclusively used for generating moves.
     */
    RandomGenerator moveIteratorUsage();

    /**
     * Used by factories before solving starts.
     *
     * @return A {@link RandomGenerator} exclusively used by factories.
     */
    RandomGenerator factoryUsage();

    /**
     * Used by {@link Acceptor} and other components inbetween move generation.
     *
     * @return A {@link RandomGenerator} exclusively used by acceptors.
     */
    RandomGenerator acceptorUsage();
}

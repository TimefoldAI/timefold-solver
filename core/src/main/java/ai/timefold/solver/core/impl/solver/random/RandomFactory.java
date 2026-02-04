package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;

/**
 * @see DefaultRandomFactory
 */
public interface RandomFactory {

    /**
     * @return never null
     */
    RandomGenerator createRandom();

}

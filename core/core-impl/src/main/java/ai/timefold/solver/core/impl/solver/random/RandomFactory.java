package ai.timefold.solver.core.impl.solver.random;

import java.util.Random;

/**
 * @see DefaultRandomFactory
 */
public interface RandomFactory {

    /**
     * @return never null
     */
    Random createRandom();

}

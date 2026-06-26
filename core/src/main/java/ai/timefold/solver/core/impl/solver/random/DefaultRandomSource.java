package ai.timefold.solver.core.impl.solver.random;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.solver.AbstractSolver;

public record DefaultRandomSource(DelegatingSplittableRandomGenerator moveRandom,
        DelegatingSplittableRandomGenerator factoryRandom,
        DelegatingSplittableRandomGenerator acceptorRandom) implements RandomSource {
    /**
     * Return a supplier of {@link DefaultRandomSource} with the specified
     * seed with the seed included in {@link Object#toString()}.
     * <p>
     * Required so the {@link RandomGenerator} is created in the thread {@link AbstractSolver#solve}
     * is called.
     *
     * @param seed The seed to use for the {@link RandomGenerator}.
     * @return A supplier include the seed in its {@link Object#toString()}
     */
    public static Supplier<RandomSource> seededSupplier(long seed) {
        return new Supplier<>() {
            @Override
            public RandomSource get() {
                return seeded(seed);
            }

            @Override
            public String toString() {
                return "seed %d".formatted(seed);
            }
        };
    }

    public static DefaultRandomSource seeded(long seed) {
        var moveRandom = new DelegatingSplittableRandomGenerator(seed);
        var factoryRandom = new DelegatingSplittableRandomGenerator(seed, moveRandom.split());
        var acceptorRandom = new DelegatingSplittableRandomGenerator(seed, moveRandom.split());
        return new DefaultRandomSource(moveRandom, factoryRandom, acceptorRandom);
    }

    public DefaultRandomSource split() {
        var newMoveRandom = new DelegatingSplittableRandomGenerator(moveRandom.getSeed(), moveRandom.split());
        var newFactoryRandom = new DelegatingSplittableRandomGenerator(moveRandom.getSeed(), newMoveRandom.split());
        var newAcceptorRandom = new DelegatingSplittableRandomGenerator(moveRandom.getSeed(), newMoveRandom.split());
        return new DefaultRandomSource(newMoveRandom, newFactoryRandom, newAcceptorRandom);
    }

    public RandomGenerator.SplittableGenerator saveState() {
        return moveRandom.split();
    }

    public void restoreState(RandomGenerator.SplittableGenerator newSourceRandom) {
        moveRandom.setDelegate(newSourceRandom);
        factoryRandom.setDelegate(newSourceRandom.split());
        acceptorRandom.setDelegate(newSourceRandom.split());
    }

    @Override
    public RandomGenerator moveIteratorUsage() {
        return moveRandom;
    }

    @Override
    public RandomGenerator factoryUsage() {
        return factoryRandom;
    }

    @Override
    public RandomGenerator acceptorUsage() {
        return acceptorRandom;
    }
}

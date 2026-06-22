package ai.timefold.solver.core.impl.solver.random;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.solver.AbstractSolver;

public record DefaultRandomSource(DelegatingSplittableRandomGenerator sourceRandom,
        DelegatingSplittableRandomGenerator splitRandom) implements RandomSource {
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
        var sourceRandom = new DelegatingSplittableRandomGenerator(seed);
        var splitRandom = new DelegatingSplittableRandomGenerator(seed, sourceRandom.split());
        return new DefaultRandomSource(sourceRandom, splitRandom);
    }

    public DefaultRandomSource split() {
        var newSourceRandom = new DelegatingSplittableRandomGenerator(sourceRandom.getSeed(), sourceRandom.split());
        var newSplitRandom = new DelegatingSplittableRandomGenerator(sourceRandom.getSeed(), newSourceRandom.split());
        return new DefaultRandomSource(newSourceRandom, newSplitRandom);
    }

    public RandomGenerator.SplittableGenerator saveState() {
        return sourceRandom.split();
    }

    public void restoreState(RandomGenerator.SplittableGenerator newSourceRandom) {
        sourceRandom.setDelegate(newSourceRandom);
        splitRandom.setDelegate(newSourceRandom.split());
    }

    @Override
    public RandomGenerator moveUsage() {
        return sourceRandom;
    }

    @Override
    public RandomGenerator acceptorUsage() {
        return splitRandom;
    }
}

package ai.timefold.solver.core.impl.solver.random;

import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.AbstractSolver;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link RandomGenerator} that delegates to another {@link RandomGenerator.SplittableGenerator}
 * instance. This allows us to change the {@link RandomGenerator} used even when
 * {@link MoveSelector} and other classes to cache the {@link RandomGenerator} in a field.
 *
 * @apiNote To ensure reproducibility, this class can only be used by the {@link Thread}
 *          that created it. Attempting to call any method from another thread will
 *          throw an {@link IllegalStateException}.
 */
@NullMarked
public final class DelegatingSplittableRandomGenerator implements RandomGenerator {
    private RandomGenerator.SplittableGenerator delegate;
    private final Thread ownerThread;
    private final long seed;

    public DelegatingSplittableRandomGenerator(long seed) {
        this.delegate = new SplittableRandom(seed);
        this.ownerThread = Thread.currentThread();
        this.seed = seed;
    }

    public DelegatingSplittableRandomGenerator(long seed, RandomGenerator.SplittableGenerator delegate) {
        this.delegate = delegate;
        this.ownerThread = Thread.currentThread();
        this.seed = seed;
    }

    private void assertIsOwnedByCurrentThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "The calling thread (%s) is not the owner thread (%s). Maybe create your own RandomGenerator instance?"
                            .formatted(Thread.currentThread(), ownerThread));
        }
    }

    public RandomGenerator.SplittableGenerator split() {
        assertIsOwnedByCurrentThread();
        return delegate.split();
    }

    public long getSeed() {
        return seed;
    }

    public RandomGenerator.SplittableGenerator getDelegate() {
        assertIsOwnedByCurrentThread();
        return delegate;
    }

    public void setDelegate(RandomGenerator.SplittableGenerator delegate) {
        assertIsOwnedByCurrentThread();
        this.delegate = delegate;
    }

    // *****************************************
    // RandomGenerator methods
    // *****************************************

    @Override
    public long nextLong() {
        assertIsOwnedByCurrentThread();
        return delegate.nextLong();
    }

    @Override
    public int nextInt() {
        assertIsOwnedByCurrentThread();
        return delegate.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextInt(bound);
    }

    @Override
    public int nextInt(int origin, int bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextInt(origin, bound);
    }

    @Override
    public long nextLong(long bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextLong(bound);
    }

    @Override
    public long nextLong(long origin, long bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextLong(origin, bound);
    }

    @Override
    public double nextDouble() {
        assertIsOwnedByCurrentThread();
        return delegate.nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextDouble(bound);
    }

    @Override
    public double nextDouble(double origin, double bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextDouble(origin, bound);
    }

    @Override
    public float nextFloat() {
        assertIsOwnedByCurrentThread();
        return delegate.nextFloat();
    }

    @Override
    public float nextFloat(float bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextFloat(bound);
    }

    @Override
    public float nextFloat(float origin, float bound) {
        assertIsOwnedByCurrentThread();
        return delegate.nextFloat(origin, bound);
    }

    @Override
    public double nextGaussian() {
        assertIsOwnedByCurrentThread();
        return delegate.nextGaussian();
    }

    @Override
    public boolean nextBoolean() {
        assertIsOwnedByCurrentThread();
        return delegate.nextBoolean();
    }

    @Override
    public void nextBytes(byte[] bytes) {
        assertIsOwnedByCurrentThread();
        delegate.nextBytes(bytes);
    }

    /**
     * Return a supplier of {@link DelegatingSplittableRandomGenerator} with the specified
     * seed with the seed included in {@link Object#toString()}.
     * <p>
     * Required so the {@link RandomGenerator} is created in the thread {@link AbstractSolver#solve}
     * is called.
     *
     * @param seed The seed to use for the {@link RandomGenerator}.
     * @return A supplier include the seed in its {@link Object#toString()}
     */
    public static Supplier<RandomGenerator> getSupplier(long seed) {
        return new Supplier<>() {
            @Override
            public RandomGenerator get() {
                return new DelegatingSplittableRandomGenerator(seed);
            }

            @Override
            public String toString() {
                return "seed %d".formatted(seed);
            }
        };
    }

    @Override
    public String toString() {
        return "%s (%s) with seed %d".formatted(delegate.getClass().getSimpleName(),
                delegate, seed);
    }
}

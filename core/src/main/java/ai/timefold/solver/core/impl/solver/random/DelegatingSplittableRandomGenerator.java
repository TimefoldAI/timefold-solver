package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link RandomGenerator} that delegates to another {@link RandomGenerator.SplittableGenerator}
 * instance. This allows us to change the {@link RandomGenerator} used even when
 * {@link MoveSelector} and other classes to cache the {@link RandomGenerator} in a field.
 * <p>
 * To ensure reproducibility, this class can only be used by the {@link Thread}
 * that created it. Attempting to call any method from another thread will
 * throw an {@link IllegalStateException}.
 */
@NullMarked
public final class DelegatingSplittableRandomGenerator implements RandomGenerator {

    /**
     * {@code SplittableRandom} belongs to the legacy group of generators, next to {@code Random}.
     * LXM replaces it, and the solver splits the generator
     * per usage, per step, per child thread and per partition.
     *
     * @see <a href="https://openjdk.org/jeps/356">JEP 356: Enhanced Pseudo-Random Number Generators</a>
     */
    private static final RandomGeneratorFactory<RandomGenerator.SplittableGenerator> DELEGATE_FACTORY =
            RandomGeneratorFactory.of("L64X128MixRandom");

    private RandomGenerator.SplittableGenerator delegate;
    private final Thread ownerThread;
    private final long seed;

    public DelegatingSplittableRandomGenerator(long seed) {
        this(seed, DELEGATE_FACTORY.create(seed));
    }

    public DelegatingSplittableRandomGenerator(long seed, RandomGenerator.SplittableGenerator delegate) {
        // No entry point may step around the improved bounding.
        this.delegate = bounded(delegate);
        this.ownerThread = Thread.currentThread();
        this.seed = seed;
    }

    private static RandomGenerator.SplittableGenerator bounded(RandomGenerator.SplittableGenerator generator) {
        return generator instanceof BoundedSplittableGenerator ? generator : new BoundedSplittableGenerator(generator);
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
        this.delegate = bounded(delegate);
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

    // The JDK builds these streams on the unbounded nextInt()/nextLong(),
    // which would step around both the bounded draw and the owner thread check.
    // Going through this class's own bounded methods keeps both.

    @Override
    public IntStream ints(int origin, int bound) {
        assertIsOwnedByCurrentThread();
        return IntStream.generate(() -> nextInt(origin, bound));
    }

    @Override
    public IntStream ints(long streamSize, int origin, int bound) {
        assertIsOwnedByCurrentThread();
        return LongStream.range(0L, streamSize)
                .mapToInt(ignored -> nextInt(origin, bound));
    }

    @Override
    public LongStream longs(long origin, long bound) {
        assertIsOwnedByCurrentThread();
        return LongStream.generate(() -> nextLong(origin, bound));
    }

    @Override
    public LongStream longs(long streamSize, long origin, long bound) {
        assertIsOwnedByCurrentThread();
        return LongStream.range(0L, streamSize)
                .map(ignored -> nextLong(origin, bound));
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

    @Override
    public String toString() {
        return "%s (%s) with seed %d".formatted(delegate.getClass().getSimpleName(),
                delegate, seed);
    }
}

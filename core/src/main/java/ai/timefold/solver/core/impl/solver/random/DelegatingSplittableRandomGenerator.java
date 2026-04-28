package ai.timefold.solver.core.impl.solver.random;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class DelegatingSplittableRandomGenerator implements RandomGenerator {
    private RandomGenerator.SplittableGenerator delegate;
    private final Thread ownerThread;

    public DelegatingSplittableRandomGenerator(long seed) {
        this.delegate = new SplittableRandom(seed);
        this.ownerThread = Thread.currentThread();
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
}

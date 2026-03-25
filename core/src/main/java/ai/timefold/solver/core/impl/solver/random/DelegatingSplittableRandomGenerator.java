package ai.timefold.solver.core.impl.solver.random;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class DelegatingSplittableRandomGenerator implements RandomGenerator {
    RandomGenerator.SplittableGenerator delegate;

    DelegatingSplittableRandomGenerator(long seed) {
        this.delegate = new SplittableRandom(seed);
    }

    public RandomGenerator.SplittableGenerator split() {
        return delegate.split();
    }

    public void setDelegate(RandomGenerator.SplittableGenerator delegate) {
        this.delegate = delegate;
    }

    // *****************************************
    // RandomGenerator methods
    // *****************************************

    @Override
    public long nextLong() {
        return delegate.nextLong();
    }

    @Override
    public int nextInt() {
        return delegate.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return delegate.nextInt(bound);
    }

    @Override
    public int nextInt(int origin, int bound) {
        return delegate.nextInt(origin, bound);
    }

    @Override
    public long nextLong(long bound) {
        return delegate.nextLong(bound);
    }

    @Override
    public long nextLong(long origin, long bound) {
        return delegate.nextLong(origin, bound);
    }

    @Override
    public double nextDouble() {
        return delegate.nextDouble();
    }

    @Override
    public double nextDouble(double bound) {
        return delegate.nextDouble(bound);
    }

    @Override
    public double nextDouble(double origin, double bound) {
        return delegate.nextDouble(origin, bound);
    }

    @Override
    public float nextFloat() {
        return delegate.nextFloat();
    }

    @Override
    public float nextFloat(float bound) {
        return delegate.nextFloat(bound);
    }

    @Override
    public float nextFloat(float origin, float bound) {
        return delegate.nextFloat(origin, bound);
    }

    @Override
    public double nextGaussian() {
        return delegate.nextGaussian();
    }

    @Override
    public boolean nextBoolean() {
        return delegate.nextBoolean();
    }

    @Override
    public void nextBytes(byte[] bytes) {
        delegate.nextBytes(bytes);
    }
}

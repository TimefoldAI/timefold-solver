package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Stops a sample once it reaches a target size drawn uniformly from {@code [minimumSize, maximumSize]},
 * a fresh draw per sample.
 */
@NullMarked
final class DefaultSampler<A> implements Sampler<A> {

    private final int minimumSize;
    private final int maximumSize;
    private int targetSize;

    DefaultSampler(int minimumSize, int maximumSize) {
        if (minimumSize < 1) {
            throw new IllegalArgumentException("The minimumSize (%d) of a sampler must be at least 1."
                    .formatted(minimumSize));
        }
        if (maximumSize < minimumSize) {
            throw new IllegalArgumentException(
                    "The maximumSize (%d) of a sampler must be at least the minimumSize (%d)."
                            .formatted(maximumSize, minimumSize));
        }
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
        this.targetSize = minimumSize;
    }

    @Override
    public void reset(RandomGenerator random) {
        targetSize = minimumSize == maximumSize ? minimumSize : random.nextInt(minimumSize, maximumSize + 1);
    }

    @Override
    public int minimumSize() {
        return minimumSize;
    }

    @Override
    public int targetSize() {
        return targetSize;
    }

    @Override
    public Decision evaluate(int sizeSoFar, @Nullable A candidate) {
        return sizeSoFar + 1 >= targetSize ? Decision.ACCEPT_AND_STOP : Decision.ACCEPT;
    }

}

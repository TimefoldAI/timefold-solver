package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Ready-made {@link Sampler}, {@link PillarSampler} and {@link SubListSampler} implementations.
 */
@NullMarked
public final class Samplers {

    /**
     * @return a sampler that accepts every candidate offered,
     *         so a sample drains its whole source or slice
     */
    public static <A> Sampler<A> all() {
        return (sizeSoFar, candidate) -> Decision.ACCEPT;
    }

    /**
     * As defined by {@link #all()},
     * but reporting an expected sample size.
     * An unbounded sampler cannot know its own size,
     * so the caller supplies it;
     * it only sizes the collection the sample is assembled in,
     * and it costs nothing but a resize if it is wrong.
     *
     * @param expectedSize the number of members a sample is expected to hold; at least 1
     * @return a sampler that accepts every candidate offered,
     *         so a sample drains its whole source or slice
     * @throws IllegalArgumentException if expectedSize is below 1
     */
    public static <A> Sampler<A> all(int expectedSize) {
        if (expectedSize < 1) {
            throw new IllegalArgumentException(
                    "The expectedSize (%d) of a sampler must be at least 1.".formatted(expectedSize));
        }
        return new Sampler<A>() {

            @Override
            public int targetSize() {
                return expectedSize;
            }

            @Override
            public Decision evaluate(int sizeSoFar, @Nullable A candidate) {
                return Decision.ACCEPT;
            }

        };
    }

    /**
     * A source or slice smaller than {@code size} yields no sample at all;
     * see {@link #between(int, int)}, of which this is a special case.
     *
     * @param size the exact number of members every sample will have; at least 1
     * @return a sampler that stops a sample as soon as it reaches {@code size} members
     */
    public static <A> Sampler<A> exactly(int size) {
        return new DefaultSampler<>(size, size);
    }

    /**
     * @param maximumSize the largest number of members a sample may have; at least 1
     * @return a sampler that stops a sample
     *         once it reaches a size drawn uniformly from {@code [1, maximumSize]}, a fresh draw per sample
     */
    public static <A> Sampler<A> upTo(int maximumSize) {
        return new DefaultSampler<>(1, maximumSize);
    }

    /**
     * A slice smaller than {@code minimumSize} yields no sample at all,
     * and still costs a full drain of that slice to find out -
     * keep {@code minimumSize} near the smallest group actually worth moving.
     *
     * @param minimumSize the smallest number of members a sample may have; at least 1
     * @param maximumSize the largest number of members a sample may have; at least {@code minimumSize}
     * @return a sampler that stops a sample
     *         once it reaches a size drawn uniformly from {@code [minimumSize, maximumSize]}, a fresh draw per sample
     */
    public static <A> Sampler<A> between(int minimumSize, int maximumSize) {
        return new DefaultSampler<>(minimumSize, maximumSize);
    }

    /**
     * Lifts a key-blind {@link Sampler} into a {@link PillarSampler}, ignoring whatever key it is drawn under.
     * A key-aware policy implements {@link PillarSampler} directly instead.
     *
     * @param sampler never null
     * @return never null
     */
    public static <Key_, A> PillarSampler<Key_, A> pillar(Sampler<A> sampler) {
        Objects.requireNonNull(sampler);
        return new PillarSampler<>() {

            @Override
            public void reset(RandomGenerator random, @Nullable Key_ key) {
                sampler.reset(random);
            }

            @Override
            public int minimumSize() {
                return sampler.minimumSize();
            }

            @Override
            public int targetSize() {
                return sampler.targetSize();
            }

            @Override
            public Decision evaluate(int sizeSoFar, @Nullable A candidate) {
                return sampler.evaluate(sizeSoFar, candidate);
            }

        };
    }

    /**
     * The only way to build a {@link SubListSampler}.
     * The caller must already hold the working random,
     * so in practice this is called by a move provider, not directly by user code.
     *
     * @return never null
     */
    public static <Solution_, Entity_, Value_> SubListSampler<Solution_, Entity_, Value_> subList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize, RandomGenerator random) {
        return new DefaultSubListSampler<>(variableMetaModel, minimumSubListSize, maximumSubListSize, random);
    }

    private Samplers() {
        // No external instances.
    }

}

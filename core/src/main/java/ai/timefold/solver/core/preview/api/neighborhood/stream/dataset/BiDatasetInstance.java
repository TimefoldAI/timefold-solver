package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.sample.SampleAssembler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link BiDataset}, resolved against a specific solving session.
 *
 * @param <A> the type of the dataset's left rows
 * @param <B> the type of the dataset's right rows
 */
@NullMarked
public interface BiDatasetInstance<A, B> {

    /**
     * As defined by {@link UniDatasetInstance#size()}.
     */
    int size();

    /**
     * As defined by {@link UniDatasetInstance#iterator(RandomGenerator)}.
     */
    BiIterator<A, B> iterator(RandomGenerator random);

    /**
     * As defined by {@link UniDatasetInstance#exhaustiveIterator(RandomGenerator)}.
     */
    BiIterator<A, B> exhaustiveIterator(RandomGenerator random);

    /**
     * As defined by {@link #size()},
     * but restricted to rows paired with the given left value.
     */
    int size(@Nullable A a);

    /**
     * As defined by {@link #iterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> iterator(@Nullable A a, RandomGenerator random);

    /**
     * As defined by {@link #exhaustiveIterator(RandomGenerator)},
     * but restricted to rows paired with the given left value.
     */
    Iterator<@Nullable B> exhaustiveIterator(@Nullable A a, RandomGenerator random);

    /**
     * As defined by {@link UniDatasetInstance#samplingIterator(Sampler, RandomGenerator)},
     * but restricted to rows paired with the given left value,
     * which is a slice selector and not a key:
     * the resulting {@link Sample} carries no key and promises nothing beyond its membership.
     * <p>
     * On an {@code equal}-indexed slice, a sample of k members costs about k draws,
     * and O(1) to create the draw.
     * On a comparison- or range-indexed slice, add O(b), b being the number of matching buckets,
     * because the underlying iterator walks every bucket up front.
     * <p>
     * There is a second overload of this method taking a {@link PillarSampler} instead,
     * which sees the left value as a key.
     * The two sampler types are unrelated,
     * so a bare lambda argument does not compile as ambiguous;
     * pass one built by {@link Samplers}, such as {@link Samplers#all()} or {@link Samplers#pillar(Sampler)}.
     *
     * @param a the slice selector; may be null, as null rows are legal
     * @param sampler decides which candidates join each sample
     * @param random never null
     * @return never null
     */
    default Iterator<Sample<B>> samplingIterator(@Nullable A a, Sampler<B> sampler, RandomGenerator random) {
        // Must be exhaustiveIterator, never iterator:
        // the with-replacement draw wraps a filtering() join in a FilteringIterator that has a bail-out,
        // which reports "nothing left" while elements remain,
        // so a sample would end early and silently.
        return SampleAssembler.iterator(() -> exhaustiveIterator(a, random), random, sampler);
    }

    /**
     * As defined by {@link #samplingIterator(Object, Sampler, RandomGenerator)},
     * but for a {@link PillarSampler}, which sees the left value {@code a} as the key the sample is drawn under -
     * still a slice selector, not a key the resulting {@link Sample} itself carries.
     * <p>
     * The two sampler types are unrelated, so a bare lambda argument does not compile as ambiguous;
     * pass one built by {@link Samplers}, such as {@link Samplers#pillar(Sampler)}.
     *
     * @param a the slice selector; may be null, as null rows are legal
     * @param sampler decides which candidates join each sample
     * @param random never null
     * @return never null
     */
    default Iterator<Sample<B>> samplingIterator(@Nullable A a, PillarSampler<A, B> sampler, RandomGenerator random) {
        // Must be exhaustiveIterator, never iterator: see the other overload's comment.
        return SampleAssembler.iterator(() -> exhaustiveIterator(a, random), random, a, sampler);
    }

}

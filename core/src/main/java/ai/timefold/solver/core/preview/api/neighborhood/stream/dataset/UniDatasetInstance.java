package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.sample.SampleAssembler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The runtime instance of a {@link UniDataset}, resolved against a specific solving session.
 *
 * @param <A> the type of the dataset's rows
 */
@NullMarked
public interface UniDatasetInstance<A> {

    /**
     * Returns a theoretical upper bound on the number of rows;
     * {@code filtering()} joiners are not accounted for, so the iterator may yield fewer.
     * This method can be very expensive,
     * as it may require traversing the entire dataset.
     */
    int size();

    /**
     * Returns an iterator performing sampling with replacement:
     * a random walk over the dataset, never an indexed ({@code get(i)}) random access.
     * Never ends, and may return the same row more than once;
     * {@link Iterator#remove()} is not supported.
     * <p>
     * This is the cheap default; prefer it over {@link #exhaustiveIterator(RandomGenerator)}
     * unless the caller specifically needs every row exactly once.
     *
     * @param random never null
     * @return never null
     */
    Iterator<@Nullable A> iterator(RandomGenerator random);

    /**
     * As defined by {@link #iterator(RandomGenerator)},
     * but performing sampling without replacement:
     * every row is eventually returned exactly once,
     * and then the iterator ends, without any cooperation from the caller.
     * {@link Iterator#remove()} is not supported and never needs to be called;
     * the caller cannot break uniqueness.
     * Draining it in full is significantly more expensive than {@link #iterator(RandomGenerator)},
     * to the point where large datasets may become impractical in terms of memory and CPU,
     * especially in the case of large multi-leveled joins.
     * Partial consumption is cheap, however:
     * pulling k of n rows and abandoning the rest costs O(1) to create plus about k draws,
     * because the underlying iterator grows its bookkeeping with the number of draws,
     * not with the size of the source.
     *
     * @param random never null
     * @return never null
     */
    Iterator<@Nullable A> exhaustiveIterator(RandomGenerator random);

    /**
     * Samples with replacement: sets of rows drawn together by the given sampler.
     * Never ends while the dataset is not empty, and may return equal samples.
     * Within one sample, members are drawn without replacement,
     * and every candidate drawn is offered to the sampler exactly once,
     * whether the sampler takes it or not.
     * Retirement is local to one sample; the next sample starts from a full source.
     * <p>
     * Each sample of k members costs about k draws plus whatever the sampler rejects;
     * the framework never rejects a candidate itself.
     * A Uni dataset has no key, so this method has no {@link PillarSampler} form.
     * Prefer {@link BiDatasetInstance#samplingIterator(Object, Sampler, RandomGenerator)}
     * when the grouping is known at build time, so that the index does the filtering;
     * use this form when membership can only be decided at run time.
     * <p>
     * Move generation is single-threaded;
     * see {@link Sampler} for what that means for sampler state.
     *
     * @param sampler decides which candidates join each sample
     * @param random never null
     * @return never null
     */
    default Iterator<Sample<A>> samplingIterator(Sampler<A> sampler, RandomGenerator random) {
        // Must be exhaustiveIterator, never iterator:
        // the with-replacement draw wraps a filtering() join in a FilteringIterator that has a bail-out,
        // which reports "nothing left" while elements remain,
        // so a sample would end early and silently.
        return SampleAssembler.iterator(() -> exhaustiveIterator(random), random, sampler);
    }

}

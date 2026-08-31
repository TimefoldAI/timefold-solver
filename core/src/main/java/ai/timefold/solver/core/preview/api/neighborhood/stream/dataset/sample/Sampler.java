package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Decides which candidates join a {@link Sample}.
 * Ready-made implementations are in {@link Samplers}.
 * <p>
 * {@link #reset(RandomGenerator)} runs once per sample,
 * before the first {@link #evaluate(int, Object)} call,
 * and {@link #evaluate(int, Object)} is then called for every candidate offered,
 * starting with {@code sizeSoFar == 0}.
 * A sample ends on {@link Decision#STOP}, {@link Decision#ACCEPT_AND_STOP}, or when the source runs out.
 * A sampler which never stops yields the whole dataset or the whole slice.
 * <p>
 * A sample with fewer members than {@link #minimumSize()} is discarded entirely -
 * as if the source had been empty.
 * If the sampler itself stops the sample (via {@link Decision#STOP} or {@link Decision#ACCEPT_AND_STOP})
 * while still below its own {@link #minimumSize()}, that is treated as a contract violation,
 * not as an undersized world, and fails fast.
 * <p>
 * A sample is assembled in full before it is returned,
 * so one sampler instance may serve several {@link Iterator}s in sequence,
 * one sample at a time.
 * <p>
 * Move generation is single-threaded,
 * and {@link #reset(RandomGenerator)} runs before every sample
 * and completes before {@link #evaluate(int, Object)} is called for that sample,
 * so a sampler may hold state across {@link #evaluate(int, Object)} calls
 * without it ever leaking into another sample.
 * This makes a sampler instance safe to share between separate neighborhood providers.
 *
 * @param <A> the type of the sample's members
 */
@NullMarked
public interface Sampler<A> {

    /**
     * Called once per sample, before the first {@link #evaluate(int, Object)} call.
     *
     * @param random the solver's working random; safe to draw from to decide this sample's target size
     *        or any other per-sample state
     */
    default void reset(RandomGenerator random) {
        // Nothing to do by default.
    }

    /**
     * @return the smallest number of members a sample may have; at least 1.
     *         A sample with fewer members, however it ended, is discarded.
     */
    default int minimumSize() {
        return 1;
    }

    /**
     * @param sizeSoFar the number of distinct members already accepted; 0 for the first candidate offered.
     *        A duplicate acceptance (the source offers a candidate equal to one already accepted) does not advance this count,
     *        since the assembled sample deduplicates.
     * @param candidate the candidate offered, drawn at most once per sample
     * @return what to do with the candidate
     */
    Decision evaluate(int sizeSoFar, @Nullable A candidate);

}

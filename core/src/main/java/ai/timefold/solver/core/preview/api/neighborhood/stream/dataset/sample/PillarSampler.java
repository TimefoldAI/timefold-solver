package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Decides which candidates join a {@link Sample} drawn under a key,
 * such as a slice of a {@link BiDatasetInstance}.
 * Unrelated to {@link Sampler}:
 * a plain, key-blind size policy is lifted into one via {@link Samplers#pillar(Sampler)}.
 * <p>
 * Otherwise follows {@link Sampler}'s contract exactly:
 * {@link #reset(RandomGenerator, Object)} runs once per sample before the first {@link #evaluate(int, Object)} call,
 * which is then called for every candidate offered starting at {@code sizeSoFar == 0};
 * a sample below {@link #minimumSize()} is discarded,
 * and the sampler stopping itself below its own {@link #minimumSize()} is a contract violation.
 * No built-in sampler reads the key today -
 * the split exists to keep this family apart from {@link Sampler} in the type system,
 * not because a key-aware policy exists yet.
 * Move generation is single-threaded;
 * see {@link Sampler} for what that means for sampler state.
 *
 * @param <Key_> the type of the key the sample was drawn under, such as the slice selector
 * @param <A> the type of the sample's members
 */
@NullMarked
public interface PillarSampler<Key_, A> {

    /**
     * Called once per sample, before the first {@link #evaluate(int, Object)} call.
     *
     * @param random the solver's working random; safe to draw from to decide this sample's target size
     *        or any other per-sample state
     * @param key the slice selector the sample is being drawn under; may be null,
     *        as a null slice selector is legal
     */
    default void reset(RandomGenerator random, @Nullable Key_ key) {
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
     * A hint at how many members this sample will hold,
     * read after {@link #reset(RandomGenerator, Object)} and before the first {@link #evaluate(int, Object)} call.
     * It only sizes the collection the sample is assembled in,
     * so an under-estimate costs a resize and nothing else.
     * A sampler which cannot know its own size in advance reports its {@link #minimumSize()}.
     *
     * @return at least {@link #minimumSize()}
     */
    default int targetSize() {
        return minimumSize();
    }

    /**
     * @param sizeSoFar the number of members already accepted; 0 for the first candidate offered
     * @param candidate the candidate offered, drawn at most once per sample
     * @return what to do with the candidate
     */
    Decision evaluate(int sizeSoFar, @Nullable A candidate);

}

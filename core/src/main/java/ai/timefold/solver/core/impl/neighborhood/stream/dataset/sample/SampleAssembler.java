package ai.timefold.solver.core.impl.neighborhood.stream.dataset.sample;

import static ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision.ACCEPT;
import static ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision.ACCEPT_AND_STOP;
import static ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision.STOP;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ScalingOrderedSet;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SampleAssembler {

    /**
     * Assembles one sample, eagerly.
     * <p>
     * The source iterator must be an exhaustive (without-replacement) iterator:
     * it retires every element it hands out, so a candidate is offered to a given sample exactly once,
     * and it has no bail-out, so it does not report "nothing left" while elements remain.
     *
     * @return null if the source is empty,
     *         if the sampler refuses the sample,
     *         or if the sample ends below the sampler's {@link Sampler#minimumSize()}
     */
    public static <A> @Nullable Sample<A> assemble(Iterator<@Nullable A> sourceIterator, RandomGenerator random,
            Sampler<A> sampler) {
        sampler.reset(random);
        return assemble(sourceIterator, sampler.minimumSize(), sampler.targetSize(), sampler, sampler);
    }

    /**
     * As defined by {@link #assemble(Iterator, RandomGenerator, Sampler)},
     * but for a {@link PillarSampler} drawn under a key.
     */
    public static <Key_, A> @Nullable Sample<A> assemble(Iterator<@Nullable A> sourceIterator, RandomGenerator random,
            @Nullable Key_ key, PillarSampler<Key_, A> sampler) {
        sampler.reset(random, key);
        return assemble(sourceIterator, sampler.minimumSize(), sampler.targetSize(), sampler, sampler::evaluate);
    }

    private static <A> @Nullable Sample<A> assemble(Iterator<@Nullable A> sourceIterator, int minimumSize, int targetSize,
            Object sampler, Sampler<A> evaluator) {
        if (minimumSize < 1) {
            throw new IllegalArgumentException("The minimumSize (%d) of sampler (%s) must be at least 1."
                    .formatted(minimumSize, sampler));
        }
        if (targetSize < minimumSize) {
            throw new IllegalArgumentException(
                    "The targetSize (%d) of sampler (%s) must be at least the minimumSize (%d)."
                            .formatted(targetSize, sampler, minimumSize));
        }
        // A set, not a list: sizeSoFar passed to evaluate() and the minimumSize check below must both see the distinct member count,
        // since Sample.of() deduplicates anyway - otherwise an accepted duplicate would consume a slot the sampler believes it filled.
        // A ScalingOrderedSet, not a LinkedHashSet: a sample this small pays no hash node at all,
        // and it is a SequencedSet, so Sample.of() adopts it instead of copying it back into a LinkedHashSet.
        var memberSet = new ScalingOrderedSet<@Nullable A>(targetSize);
        var stoppedBySampler = false;
        while (sourceIterator.hasNext()) {
            var candidate = sourceIterator.next();
            var decision = evaluator.evaluate(memberSet.size(), candidate);
            if (decision == ACCEPT || decision == ACCEPT_AND_STOP) {
                // A duplicate candidate (possible with concat/join sources) does not grow memberSet,
                // so an ACCEPT_AND_STOP on one must not end the sample early: the sampler's size
                // accounting assumed a new member, per the contract documented on Sample.Decision.
                var isNewMember = memberSet.add(candidate);
                if (decision == ACCEPT_AND_STOP && isNewMember) {
                    stoppedBySampler = true;
                    break;
                }
                continue;
            }
            if (decision == STOP) {
                stoppedBySampler = true;
                break;
            }
        }
        if (memberSet.size() >= minimumSize) {
            // memberSet is already a deduplicated, order-stable SequencedSet owned exclusively by this method,
            // which never touches it again - Sample.of() adopts a SequencedSet like this one instead of copying it.
            return Sample.of(memberSet);
        }
        if (stoppedBySampler) {
            // The sampler chose to stop itself, below its own declared floor:
            // a contract bug, not an undersized world.
            // A dry source without a sampler-initiated STOP falls through to the silent null below instead.
            throw new IllegalStateException(
                    "The sampler (%s) stopped the sample at size (%d), below its own minimumSize (%d)."
                            .formatted(sampler, memberSet.size(), minimumSize));
        }
        // Source ran dry before reaching the minimum; not the sampler's fault.
        return null;
    }

    /**
     * Samples with replacement:
     * never ends unless the source is empty or every draw is refused
     * (a sampler refusal, or a sample below the sampler's {@link Sampler#minimumSize()});
     * may return equal samples.
     * Each sample is assembled in full before it is returned,
     * so the sampler is never left mid-sample.
     * <p>
     * {@link Iterator#hasNext()} does not call {@code size()},
     * which is documented as potentially very expensive;
     * it assembles the next sample instead.
     *
     * @param sourceSupplier builds a fresh exhaustive iterator per sample,
     *        so retirement is local to one sample and the next sample starts from a full source
     */
    public static <A> Iterator<Sample<A>> iterator(Supplier<Iterator<@Nullable A>> sourceSupplier,
            RandomGenerator random, Sampler<A> sampler) {
        return iterator(() -> assemble(sourceSupplier.get(), random, sampler));
    }

    /**
     * As defined by {@link #iterator(Supplier, RandomGenerator, Sampler)},
     * but for a {@link PillarSampler} drawn under a key.
     */
    public static <Key_, A> Iterator<Sample<A>> iterator(Supplier<Iterator<@Nullable A>> sourceSupplier,
            RandomGenerator random, @Nullable Key_ key, PillarSampler<Key_, A> sampler) {
        return iterator(() -> assemble(sourceSupplier.get(), random, key, sampler));
    }

    private static <A> Iterator<Sample<A>> iterator(Supplier<@Nullable Sample<A>> sampleSupplier) {
        return new Iterator<>() {

            private @Nullable Sample<A> pendingSample = null;

            @Override
            public boolean hasNext() {
                if (pendingSample == null) {
                    pendingSample = sampleSupplier.get();
                }
                return pendingSample != null;
            }

            @Override
            public Sample<A> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var sample = pendingSample;
                pendingSample = null;
                return sample;
            }

        };
    }

    private SampleAssembler() {
        // No external instances.
    }

}

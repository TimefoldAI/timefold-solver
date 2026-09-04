package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

import ai.timefold.solver.core.api.domain.common.Lookup;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An unordered set of values drawn together by a {@link Sampler}, to be moved as one.
 * <p>
 * A sample carries no key.
 * It promises only that it is a set of members produced by a sampler;
 * any shared property is the concern of whoever drew it.
 * A sample is immutable and safe to hold in a move,
 * but it says nothing about the solution,
 * so every value a move needs must be read from the live solution.
 * <p>
 * Two samples are equal when they hold the same members,
 * whatever the order they were drawn in.
 *
 * @param <A> the type of the sample's members
 */
@NullMarked
public final class Sample<A>
        implements Iterable<@Nullable A> {

    private final SequencedSet<@Nullable A> memberSet;
    private boolean hashCodeComputed;
    private int hashCode;

    /**
     * Creates a sample from a collection, removing duplicates,
     * so that {@link #size()} never disagrees with {@link Object#equals(Object)}.
     * A {@link SequencedSet} is adopted directly and must not be modified afterward;
     * every other collection is copied.
     *
     * @param memberCollection may contain nulls
     * @param <A> the type of the sample's members
     * @return never null
     * @throws NullPointerException if memberCollection is null
     * @throws IllegalArgumentException if memberCollection is empty
     */
    public static <A> Sample<A> of(Collection<@Nullable A> memberCollection) {
        return new Sample<>(memberCollection);
    }

    private Sample(Collection<@Nullable A> memberCollection) {
        // A SequencedSet is already deduplicated and order-stable, so it is adopted, not copied.
        // SampleAssembler relies on this: it builds exactly such a set and never touches it again afterward.
        this.memberSet = memberCollection instanceof SequencedSet<@Nullable A> sequencedSet
                ? sequencedSet
                : new LinkedHashSet<>(memberCollection);
        if (memberSet.isEmpty()) {
            throw new IllegalArgumentException("The memberCollection (%s) of a sample must not be empty."
                    .formatted(memberSet));
        }
    }

    /**
     * @return the number of members; at least 1 for a drawn sample
     */
    public int size() {
        return memberSet.size();
    }

    /**
     * @param element may be null, as null members are legal
     * @return true if the element is a member
     */
    public boolean contains(@Nullable A element) {
        return memberSet.contains(element);
    }

    /**
     * @return a representative member of the sample, typically a planning entity;
     *         which one is unspecified, beyond that a given instance returns the same one on every call.
     *         May be null, as null members are legal.
     *         Useful for reading a value every member is known to share,
     *         such as a homogeneous pillar's current variable value.
     */
    public @Nullable A representative() {
        // A SequencedSet is what makes "the same member on every call" true.
        return memberSet.getFirst();
    }

    public Sample<A> rebase(Lookup lookup) {
        var rebasedSet = new LinkedHashSet<@Nullable A>();
        for (var member : memberSet) {
            rebasedSet.add(lookup.lookUpWorkingObject(member));
        }
        return new Sample<>(rebasedSet);
    }

    /**
     *
     * @return May allow mutation; the user must not change the set.
     */
    public SequencedSet<@Nullable A> getMemberSet() {
        return memberSet;
    }

    /**
     *
     * @return May allow mutation; the user must not use this to change the set.
     */
    @Override
    public Iterator<@Nullable A> iterator() {
        return memberSet.iterator();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof Sample<?> other &&
                Objects.equals(hashCode(), other.hashCode()) && // Possibly prevents expensive equality checks.
                Objects.equals(memberSet, other.memberSet);
    }

    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            hashCodeComputed = true;
            hashCode = memberSet.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "Sample(%s)"
                .formatted(memberSet);
    }

    public enum Decision {

        /**
         * Take the candidate and continue.
         */
        ACCEPT,
        /**
         * Leave the candidate out and continue.
         */
        REJECT,
        /**
         * Take the candidate and finish the sample.
         * If the candidate is already a member (a duplicate emitted by the source iterator),
         * it does not grow the sample and is treated as {@link #ACCEPT} instead of stopping -
         * a sample is never finished early on the strength of a size count that didn't actually increase.
         */
        ACCEPT_AND_STOP,
        /**
         * Leave the candidate out and finish the sample.
         */
        STOP

    }

}

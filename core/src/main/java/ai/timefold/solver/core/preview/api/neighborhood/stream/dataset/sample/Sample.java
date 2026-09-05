package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedSet;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.util.ScalingSequencedSet;

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
 * Calling {@link #equals(Object)} may be expensive,
 * based on the type of the collection the sample was created from.
 *
 * @param <A> the type of the sample's members
 */
@NullMarked
public final class Sample<A extends @Nullable Object>
        implements Iterable<A> {

    private final SequencedCollection<@Nullable A> memberCollection;
    private boolean hashCodeComputed;
    private int hashCode;

    /**
     * Creates a sample from a collection, removing duplicates and copying it,
     * so that {@link #size()} never disagrees with {@link Object#equals(Object)},
     * and so that later mutation of the given collection never affects this sample.
     *
     * @param memberCollection may contain nulls
     * @param <A> the type of the sample's members
     * @return never null
     * @throws NullPointerException if memberCollection is null
     * @throws IllegalArgumentException if memberCollection is empty
     */
    public static <A> Sample<A> of(SequencedCollection<@Nullable A> memberCollection) {
        return Sample.wrap(new ScalingSequencedSet<>(memberCollection));
    }

    /**
     * Creates a sample from a collection, copying it
     * so that later mutation of the given collection never affects this sample.
     * The caller must guarantee both it contains no duplicate members.
     *
     * @param uniqueElementCollection may contain nulls;
     *        deliberately not {@link SequencedSet},
     *        to not exclude lists of deduplicated elements.
     * @param <A> the type of the sample's members
     * @return never null
     * @throws NullPointerException if uniqueElementCollection is null
     * @throws IllegalArgumentException if uniqueElementCollection is empty
     */
    @SuppressWarnings("unchecked")
    public static <A> Sample<A> ofUniqueElements(SequencedCollection<@Nullable A> uniqueElementCollection) {
        return Sample.wrap(Arrays.asList((A[]) uniqueElementCollection.toArray()));
    }

    /**
     * Wraps the given collection directly: no copy, no duplicate check.
     * The caller must guarantee both
     * (a) it contains no duplicate members,
     * and (b) the caller holds no other reference to it and will never mutate it again -
     * this sample aliases it for its entire lifetime.
     *
     * @param alreadyOwnedAndDuplicateFree may contain nulls
     * @param <A> the type of the sample's members
     * @return never null
     * @throws NullPointerException if alreadyOwnedAndDuplicateFree is null
     * @throws IllegalArgumentException if alreadyOwnedAndDuplicateFree is empty
     */
    public static <A> Sample<A> wrap(SequencedCollection<@Nullable A> alreadyOwnedAndDuplicateFree) {
        return new Sample<>(alreadyOwnedAndDuplicateFree);
    }

    private Sample(SequencedCollection<@Nullable A> memberCollection) {
        this.memberCollection = memberCollection;
        if (memberCollection.isEmpty()) {
            throw new IllegalArgumentException("The memberCollection (%s) of a sample must not be empty."
                    .formatted(memberCollection));
        }
    }

    /**
     * @return the number of members; at least 1 for a drawn sample
     */
    public int size() {
        return memberCollection.size();
    }

    /**
     * @param element may be null, as null members are legal
     * @return true if the element is a member
     */
    public boolean contains(@Nullable A element) {
        return memberCollection.contains(element);
    }

    /**
     * @return a representative member of the sample, typically a planning entity;
     *         which one is unspecified, beyond that a given instance returns the same one on every call.
     *         May be null, as null members are legal.
     *         Useful for reading a value every member is known to share,
     *         such as a homogeneous pillar's current variable value.
     */
    public @Nullable A representative() {
        // memberSet is never mutated after construction, which is what makes
        // "the same member on every call" true.
        return memberCollection.getFirst();
    }

    public Sample<A> rebase(Lookup lookup) {
        var rebasedCollection = memberCollection instanceof List<A> memberList ? new ArrayList<A>(memberList.size()) : // Maintain the efficiency.
                new ScalingSequencedSet<A>(memberCollection.size());
        for (var member : memberCollection) {
            rebasedCollection.add(lookup.lookUpWorkingObject(member));
        }
        return Sample.wrap(rebasedCollection);
    }

    /**
     *
     * @return The user must not mutate the returned collection,
     *         even if the collection itself allows it.
     */
    public SequencedCollection<@Nullable A> memberCollection() {
        return memberCollection;
    }

    /**
     *
     * @return The user must not mutate the underlying collection,
     *         even if the iterator itself allows it.
     */
    @Override
    public Iterator<@Nullable A> iterator() {
        return memberCollection.iterator();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        // memberSet is no longer always a real Set (it may be a duplicate-free List via wrap()),
        // so equality can't delegate to memberSet.equals() -
        // that would silently become order-sensitive.
        // Mirrors AbstractSet.equals(): size check, then mutual containment.
        return o instanceof Sample<?> other &&
                Objects.equals(hashCode(), other.hashCode()) && // Possibly prevents expensive equality checks.
                memberCollection.size() == other.memberCollection.size() &&
                other.memberCollection.containsAll(memberCollection);
    }

    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            hashCodeComputed = true;
            // Mirrors AbstractSet.hashCode(): sum of member hash codes, order-independent,
            // consistent regardless of whether memberSet is a Set or a duplicate-free List.
            var sum = 0;
            for (var member : memberCollection) {
                sum += Objects.hashCode(member);
            }
            hashCode = sum;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "Sample(%s)"
                .formatted(memberCollection);
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

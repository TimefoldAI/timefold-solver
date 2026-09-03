package ai.timefold.solver.core.impl.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedSet;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An insertion-ordered {@link SequencedSet} which scales its representation with its size,
 * so that a small set costs no hash node at all.
 * <p>
 * Four tiers, in growth order:
 * <ul>
 * <li>empty: no backing collection.
 * <li>a single element, held in a field.
 * <li>a {@link ArrayList} of up to {@value #LIST_SIZE_THRESHOLD} elements,
 * where {@link #add(Object)} scans linearly instead of hashing.
 * <li>a {@link LinkedHashSet} beyond that.
 * </ul>
 * A set built with an expected size above {@value #LIST_SIZE_THRESHOLD} starts in the last tier directly,
 * because the earlier tiers would be a guaranteed dead end.
 * <p>
 * This speeds up {@link #add(Object)} for a small set,
 * because no {@link Object#hashCode()} needs to be calculated,
 * and it removes the per-element node a {@link LinkedHashSet} allocates.
 * <p>
 * Growth through the single-element tier is one-way:
 * the set falls back from {@link LinkedHashSet} to {@link ArrayList} when it shrinks below the threshold,
 * but it never falls back to the single-element tier,
 * and it only returns to empty by {@link #clear()} or by removing the one element it holds.
 * The intended users only ever add, so the reverse path would be dead weight.
 * <p>
 * Null elements are supported,
 * which is why empty is a tier of its own rather than a null element field.
 * <p>
 * Only {@link #add(Object)}, {@link #addAll(Collection)}, {@link #remove(Object)} and {@link #clear()} mutate.
 * {@link Iterator#remove()}, {@link #removeAll(Collection)}, {@link #retainAll(Collection)},
 * {@link #addFirst(Object)} and {@link #addLast(Object)} all throw {@link UnsupportedOperationException};
 * in particular this set does not re-order an existing element the way {@link LinkedHashSet} does.
 * <p>
 * This class is so very not thread safe.
 *
 * @param <E> the element type; may be a nullable type
 */
@NullMarked
public final class ScalingOrderedSet<E extends @Nullable Object>
        extends AbstractSet<E>
        implements SequencedSet<E> {

    static final int LIST_SIZE_THRESHOLD = 16;

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private enum Tier {
        EMPTY,
        SINGLE,
        LIST,
        SET
    }

    /**
     * Sizes the {@link ArrayList} at the single-to-list transition,
     * so a set which knows it will hold 5 elements never grows its backing array.
     */
    private final int expectedSize;

    private Tier tier;
    private @Nullable E single;
    private @Nullable List<E> list;
    private @Nullable SequencedSet<E> set;

    public ScalingOrderedSet() {
        this(1);
    }

    /**
     * @param expectedSize the number of elements this set is expected to hold, at least 1;
     *        a value above {@value #LIST_SIZE_THRESHOLD} starts the set in its {@link LinkedHashSet} tier
     * @throws IllegalArgumentException if expectedSize is below 1
     */
    public ScalingOrderedSet(int expectedSize) {
        if (expectedSize < 1) {
            throw new IllegalArgumentException(
                    "The expectedSize (%d) of a ScalingOrderedSet must be at least 1.".formatted(expectedSize));
        }
        this.expectedSize = expectedSize;
        if (expectedSize > LIST_SIZE_THRESHOLD) {
            this.tier = Tier.SET;
            this.set = LinkedHashSet.newLinkedHashSet(expectedSize);
        } else {
            this.tier = Tier.EMPTY;
        }
    }

    @Override
    public int size() {
        return switch (tier) {
            case EMPTY -> 0;
            case SINGLE -> 1;
            case LIST -> list.size();
            case SET -> set.size();
        };
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return switch (tier) {
            case EMPTY -> false;
            case SINGLE -> Objects.equals(single, o);
            case LIST -> list.contains(o);
            case SET -> set.contains(o);
        };
    }

    @Override
    public boolean add(E e) {
        return switch (tier) {
            case EMPTY -> {
                single = e;
                tier = Tier.SINGLE;
                yield true;
            }
            case SINGLE -> {
                if (Objects.equals(single, e)) {
                    yield false;
                }
                // The list only ever exists with at least two elements, hence the floor of 2.
                list = new ArrayList<>(Math.clamp(expectedSize, 2, LIST_SIZE_THRESHOLD));
                list.add(single);
                list.add(e);
                single = null;
                tier = Tier.LIST;
                yield true;
            }
            case LIST -> {
                if (list.contains(e)) {
                    yield false;
                }
                if (list.size() + 1 > LIST_SIZE_THRESHOLD) {
                    set = LinkedHashSet.newLinkedHashSet(list.size() + 1);
                    set.addAll(list);
                    list = null;
                    tier = Tier.SET;
                    yield set.add(e);
                }
                yield list.add(e);
            }
            case SET -> set.add(e);
        };
    }

    @Override
    public boolean remove(Object o) {
        return switch (tier) {
            case EMPTY -> false;
            case SINGLE -> {
                if (!Objects.equals(single, o)) {
                    yield false;
                }
                single = null;
                tier = Tier.EMPTY;
                yield true;
            }
            // Stays in the list tier even when it empties; only clear() returns to the empty tier from here.
            case LIST -> list.remove(o);
            case SET -> {
                if (!set.remove(o)) {
                    yield false;
                }
                if (set.size() <= LIST_SIZE_THRESHOLD) {
                    list = new ArrayList<>(set);
                    set = null;
                    tier = Tier.LIST;
                }
                yield true;
            }
        };
    }

    @Override
    public void clear() {
        tier = Tier.EMPTY;
        single = null;
        list = null;
        set = null;
    }

    @Override
    public Iterator<E> iterator() {
        return unmodifiableIterator(switch (tier) {
            case EMPTY -> Collections.<E> emptyIterator();
            // Not List.of(), which rejects a null element.
            case SINGLE -> Collections.singletonList(single).iterator();
            case LIST -> list.iterator();
            case SET -> set.iterator();
        });
    }

    @Override
    public E getFirst() {
        return switch (tier) {
            case EMPTY -> throw new NoSuchElementException("The set is empty.");
            case SINGLE -> single;
            case LIST -> list.getFirst();
            case SET -> set.getFirst();
        };
    }

    @Override
    public E getLast() {
        return switch (tier) {
            case EMPTY -> throw new NoSuchElementException("The set is empty.");
            case SINGLE -> single;
            case LIST -> list.getLast();
            case SET -> set.getLast();
        };
    }

    /**
     * @return a live, read-only, reverse-ordered view of this set;
     *         unlike the {@link SequencedSet#reversed()} contract, the view does not write through,
     *         because nothing needs it to and a write-through would have to handle the tier transitions
     */
    @Override
    public SequencedSet<E> reversed() {
        return new ReversedView();
    }

    @Override
    public Object[] toArray() {
        return switch (tier) {
            case EMPTY -> EMPTY_ARRAY;
            case SINGLE -> new Object[] { single };
            case LIST -> list.toArray();
            case SET -> set.toArray();
        };
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return switch (tier) {
            case EMPTY -> Collections.<E> emptyList().toArray(a);
            case SINGLE -> Collections.singletonList(single).toArray(a);
            case LIST -> list.toArray(a);
            case SET -> set.toArray(a);
        };
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retainAll() not yet implemented");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll() not yet implemented");
    }

    private static <T> Iterator<T> unmodifiableIterator(Iterator<T> childIterator) {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return childIterator.hasNext();
            }

            @Override
            public T next() {
                return childIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * A live, read-only, reverse-ordered view of the enclosing set.
     * It reads the current tier on every call,
     * so it follows the enclosing set as it grows and as it scales.
     */
    private final class ReversedView extends AbstractSet<E> implements SequencedSet<E> {

        @Override
        public int size() {
            return ScalingOrderedSet.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return ScalingOrderedSet.this.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return unmodifiableIterator(switch (tier) {
                case EMPTY -> Collections.<E> emptyIterator();
                case SINGLE -> Collections.singletonList(single).iterator();
                case LIST -> list.reversed().iterator();
                case SET -> set.reversed().iterator();
            });
        }

        @Override
        public E getFirst() {
            return ScalingOrderedSet.this.getLast();
        }

        @Override
        public E getLast() {
            return ScalingOrderedSet.this.getFirst();
        }

        @Override
        public SequencedSet<E> reversed() {
            return ScalingOrderedSet.this;
        }

    }

}

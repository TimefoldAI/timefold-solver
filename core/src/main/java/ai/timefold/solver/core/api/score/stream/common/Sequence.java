package ai.timefold.solver.core.api.score.stream.common;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a series of consecutive values.
 * For instance, the list [1,2,4,5,6,10] has three sequences: [1,2], [4,5,6], and [10].
 *
 * @param <Value_> The type of value in the sequence.
 * @param <Difference_> The type of difference between values in the sequence.
 */
public interface Sequence<Value_, Difference_ extends Comparable<Difference_>> {

    /**
     * @return the first item in the sequence.
     */
    @NonNull
    Value_ getFirstItem();

    /**
     * @return the last item in the sequence.
     */
    @NonNull
    Value_ getLastItem();

    /**
     * @return true if and only if this is the first sequence
     */
    boolean isFirst();

    /**
     * @return true if and only if this is the last sequence
     */
    boolean isLast();

    /**
     * @return If this is not the first sequence, the break before it. Otherwise, null.
     */
    @Nullable
    Break<Value_, Difference_> getPreviousBreak();

    /**
     * @return If this is not the last sequence, the break after it. Otherwise, null.
     */
    @Nullable
    Break<Value_, Difference_> getNextBreak();

    /**
     * @return items in this sequence
     */
    @NonNull
    Collection<Value_> getItems();

    /**
     * @return the number of items in this sequence
     */
    int getCount();

    /**
     * @return the difference between the last item and first item in this sequence
     */
    @NonNull
    Difference_ getLength();
}

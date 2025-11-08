package ai.timefold.solver.core.impl.bavet.common.index;

import org.jspecify.annotations.NullMarked;

/**
 * Allows to read and modify the position of an element in an {@link IndexedSet}.
 * Typically points to a field in the element itself.
 *
 * @param <T>
 */
@NullMarked
public interface ElementPositionTracker<T> {

    /**
     * Sets the position of the given element.
     *
     * @param element never null
     * @param position >= 0
     */
    void setPosition(T element, int position);

    /**
     * Clears the position of the given element.
     *
     * @param element never null
     * @return the previous position of the element, or -1 if it was not tracked before
     */
    int clearPosition(T element);

}

package ai.timefold.solver.core.impl.bavet.common.index;

/**
 * Allows to read and modify the position of an element in an {@link IndexedSet}.
 * Typically points to a field in the element itself.
 *
 * @param <T>
 */
public interface ElementPositionTracker<T> {

    /**
     * Gets the position of the given element.
     *
     * @param element never null
     * @return >= 0 if the element is tracked, or -1 if it is not tracked
     */
    int getPosition(T element);

    /**
     * Sets the position of the given element.
     *
     * @param element never null
     * @param position >= 0
     * @return the previous position of the element, or -1 if it was not tracked before
     */
    int setPosition(T element, int position);

    /**
     * Clears the position of the given element.
     *
     * @param element never null
     * @return the previous position of the element, or -1 if it was not tracked before
     */
    int clearPosition(T element);

}

package ai.timefold.solver.core.impl.util;

import java.util.Arrays;

/**
 * A class representing an int array that is dynamically allocated based on the first set index.
 * The array is only created when the first element is set and is reallocated as needed
 * when lower indices are accessed.
 */
public final class DynamicIntArray {

    // Growth factor for array expansion; not too much, the point of this class is to avoid excessive memory use.
    private static final double GROWTH_FACTOR = 1.2;
    // Minimum capacity increment to avoid small incremental growth
    private static final int MIN_CAPACITY_INCREMENT = 10;

    private final ClearingStrategy clearingStrategy;
    private final int maxLength;
    private int[] array;
    private int firstIndex;
    private int lastIndex;

    public DynamicIntArray() {
        this(Integer.MAX_VALUE);
    }

    public DynamicIntArray(ClearingStrategy clearingStrategy) {
        this(Integer.MAX_VALUE, clearingStrategy);
    }

    public DynamicIntArray(int maxLength) {
        this(maxLength, ClearingStrategy.FULL);
    }

    public DynamicIntArray(int maxLength, ClearingStrategy clearingStrategy) {
        this.maxLength = maxLength;
        this.clearingStrategy = clearingStrategy;
        initializeArray();
    }

    /**
     * Sets the value at the specified index.
     * If this is the first element, the array is created.
     * If the index is lower than the current firstIndex or higher than the current lastIndex,
     * the array is reallocated with a growth strategy to reduce frequent reallocations.
     *
     * @param index the index at which to set the value
     * @param value the value to set
     */
    public void set(int index, int value) {
        if (index < 0 || index >= maxLength) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (array == null) {
            // First element, create the array with initial capacity
            var initialCapacity = Math.min(MIN_CAPACITY_INCREMENT, maxLength);
            array = new int[initialCapacity];
            firstIndex = index;
            lastIndex = index;
            array[0] = value;
        } else if (index < firstIndex) {
            // New index is lower than first index, need to reallocate
            var currentSize = lastIndex - firstIndex + 1;
            var offset = firstIndex - index;

            // Calculate new capacity with growth strategy
            var requiredCapacity = currentSize + offset;
            var newCapacity = calculateNewCapacity(requiredCapacity);

            // Copy existing elements to new array with offset
            var newArray = new int[newCapacity];
            System.arraycopy(array, 0, newArray, offset, currentSize);
            array = newArray;
            firstIndex = index;
            array[0] = value;
        } else if (index > lastIndex) {
            // New index is higher than last index, need to expand
            var currentSize = lastIndex - firstIndex + 1;
            var newSize = index - firstIndex + 1;

            if (newSize > array.length) {
                // Calculate new capacity with growth strategy
                var newCapacity = calculateNewCapacity(newSize);

                // Copy existing elements to new array
                var newArray = new int[newCapacity];
                System.arraycopy(array, 0, newArray, 0, currentSize);
                array = newArray;
            }

            // Update last index
            lastIndex = index;
            array[index - firstIndex] = value;
        } else {
            // Index is within existing range
            array[index - firstIndex] = value;
        }
    }

    /**
     * Calculates the new capacity based on the required capacity and growth strategy.
     *
     * @param requiredCapacity the minimum capacity needed
     * @return the new capacity
     */
    private int calculateNewCapacity(int requiredCapacity) {
        var currentCapacity = array != null ? array.length : 0;

        if (requiredCapacity <= currentCapacity) {
            return currentCapacity;
        }

        // Calculate new capacity using growth factor
        var newCapacity = (int) (currentCapacity * GROWTH_FACTOR);

        // Ensure minimum increment
        if (newCapacity - currentCapacity < MIN_CAPACITY_INCREMENT) {
            newCapacity = currentCapacity + MIN_CAPACITY_INCREMENT;
        }

        // Ensure new capacity is at least the required capacity
        if (newCapacity < requiredCapacity) {
            newCapacity = requiredCapacity;
        }

        // Ensure new capacity doesn't exceed maxLength
        return Math.min(newCapacity, maxLength);
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index the index from which to get the value
     * @return the value at the index
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public int get(int index) {
        if (index < 0 || index >= maxLength) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (array == null || index < firstIndex || index > lastIndex) {
            return 0;
        }
        return array[index - firstIndex];
    }

    /**
     * Checks if the array contains the specified index.
     *
     * @param index the index to check
     * @return true if the index is within bounds, false otherwise
     */
    boolean containsIndex(int index) {
        return array != null && index >= firstIndex && index <= lastIndex;
    }

    /**
     * Gets the first index of the array.
     *
     * @return the first index
     * @throws IllegalStateException if the array is empty
     */
    int getFirstIndex() {
        if (array == null) {
            throw new IllegalStateException("Array is empty");
        }
        return firstIndex;
    }

    /**
     * Gets the last index of the array.
     *
     * @return the last index
     * @throws IllegalStateException if the array is empty
     */
    int getLastIndex() {
        if (array == null) {
            throw new IllegalStateException("Array is empty");
        }
        return lastIndex;
    }

    /**
     * Clears the array by setting all values to 0.
     * The array structure is preserved, only the values are reset.
     */
    public void clear() {
        if (clearingStrategy == ClearingStrategy.FULL) {
            initializeArray();
        } else {
            // If array is null, there's nothing to clear
            if (array == null) {
                return;
            }

            // Only clear the used portion of the array (from firstIndex to lastIndex)
            // This is more efficient for large arrays with sparse indices
            Arrays.fill(array, 0, lastIndex - firstIndex + 1, 0);
        }
    }

    private void initializeArray() {
        this.array = null;
        this.firstIndex = Integer.MAX_VALUE;
        this.lastIndex = Integer.MIN_VALUE;
    }

    public enum ClearingStrategy {

        /**
         * The GC will be allowed to reclaim the array.
         * This means that, on next access, the array will have to be reallocated and gradually resized,
         * possibly leading to excessive GC pressure.
         *
         * This is the default.
         */
        FULL,
        /**
         * The array will not be returned to GC and will be filled with zeros instead.
         * This has no impact on GC, but may result in greater at-rest heap usage than strictly necessary.
         */
        PARTIAL

    }

}

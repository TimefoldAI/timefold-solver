package ai.timefold.solver.core.impl.util;

import java.util.Arrays;

/**
 * A class representing an int array that is dynamically allocated based on the first set index.
 * The array is only created when the first element is set and is reallocated as needed
 * when lower indices are accessed.
 */
public final class DynamicIntArray {

    private final int maxLength;
    private int[] array;
    private int firstIndex;
    private int lastIndex;

    public DynamicIntArray() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new empty DynamicIntArray.
     */
    public DynamicIntArray(int maxLength) {
        this.maxLength = maxLength;
        // Array is null until first element is set
        this.array = null;
        // Initialize with invalid indices
        this.firstIndex = Integer.MAX_VALUE;
        this.lastIndex = Integer.MIN_VALUE;
    }

    /**
     * Sets the value at the specified index.
     * If this is the first element, the array is created.
     * If the index is lower than the current firstIndex, the array is reallocated.
     *
     * @param index the index at which to set the value
     * @param value the value to set
     */
    public void set(int index, int value) {
        if (index < 0 || index >= maxLength) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (array == null) {
            // First element, create the array with size 1
            array = new int[1];
            firstIndex = index;
            lastIndex = index;
            array[0] = value;
        } else if (index < firstIndex) {
            // New index is lower than first index, need to reallocate
            int newSize = lastIndex - index + 1;
            int[] newArray = new int[newSize];

            // Copy existing elements to new array with offset
            int offset = firstIndex - index;
            System.arraycopy(array, 0, newArray, offset, lastIndex - firstIndex + 1);

            // Update first index and array
            firstIndex = index;
            array = newArray;
            array[0] = value;
        } else if (index > lastIndex) {
            // New index is higher than last index, need to expand
            int newSize = index - firstIndex + 1;
            int[] newArray = new int[newSize];

            // Copy existing elements to new array
            System.arraycopy(array, 0, newArray, 0, array.length);

            // Update last index and array
            lastIndex = index;
            array = newArray;
            array[index - firstIndex] = value;
        } else {
            // Index is within existing range
            array[index - firstIndex] = value;
        }
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
     * Gets the length of the array.
     *
     * @return the length of the array, or 0 if the array is empty
     */
    int length() {
        if (array == null) {
            return 0;
        }
        return lastIndex + 1;
    }

    public void clear() {
        // Fill rather than reallocate.
        // We keep the original bounds, assuming the array is likely to be filled up again.
        Arrays.fill(array, 0);
    }

}
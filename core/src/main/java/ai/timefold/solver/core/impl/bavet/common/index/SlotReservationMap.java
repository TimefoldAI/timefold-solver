package ai.timefold.solver.core.impl.bavet.common.index;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An int-to-int map over the slot range {@code [0, slotCount)},
 * in which a slot that holds no reservation resolves to itself.
 * That identity default is what {@link DefaultRetiringRandomIterator} starts with:
 * every slot holds its own logical index until a retirement moves another logical index into it.
 * <p>
 * The map has three stages, and it allocates nothing until the first reservation:
 * <ul>
 * <li><b>empty</b>: both arrays are null, and {@link #resolve(int)} is a single comparison.
 * <li><b>sparse</b>: {@link #sparseLog} holds up to {@link #maxSparseEntryCount} pairs,
 * and {@link #resolve(int)} scans them.
 * <li><b>dense</b>: {@link #denseArray} holds one entry per slot,
 * and {@link #resolve(int)} is a single array load.
 * </ul>
 * The sparse stage exists because one of these maps is built per iterator,
 * over datasets that can hold well over 100,000 tuples,
 * while most iterators retire only a handful of elements.
 * The dense stage exists because an iterator that drains its source
 * must not pay a growing scan on every draw.
 * <p>
 * This class is not thread safe.
 */
@NullMarked
final class SlotReservationMap {

    /**
     * The sparse stage upgrades to the dense stage when {@link #sparseLog} is full.
     * Keep this small: {@link #resolve(int)} scans the log, and it runs on every draw.
     */
    private static final int MAX_SPARSE_ENTRY_COUNT = 16;
    private static final int NOT_FOUND = -1;
    /**
     * {@link #toString()} renders no more reservations than this,
     * because the dense stage can hold one per slot
     * and the result goes into exception messages.
     */
    private static final int MAX_TO_STRING_RESERVATION_COUNT = 20;

    private final int slotCount;
    /**
     * Never more than {@code slotCount / 2},
     * so that {@link #sparseLog} (two ints per reservation)
     * never costs more memory than {@link #denseArray} (one int per slot) would.
     * May be zero, in which case the first reservation goes straight to the dense stage.
     */
    private final int maxSparseEntryCount;

    /**
     * Holds {@code [slot0, logicalIndex0, slot1, logicalIndex1, ...]}
     * in the prefix {@code [0, 2 * sparseEntryCount)};
     * null until the first reservation, and again after the upgrade to {@link #denseArray}.
     * Holds only the slots that deviate from the identity default.
     */
    private int @Nullable [] sparseLog = null;
    private int sparseEntryCount = 0;
    /**
     * Holds the logical index of every slot, offset by one, so that {@code 0} means the identity default;
     * null until {@link #sparseLog} overflows.
     * The JVM zero-fills a new array, so the identity default needs no explicit fill.
     */
    private int @Nullable [] denseArray = null;

    SlotReservationMap(int slotCount) {
        if (slotCount < 0) {
            throw new IllegalArgumentException("The slotCount (%d) must not be negative.".formatted(slotCount));
        }
        this.slotCount = slotCount;
        this.maxSparseEntryCount = Math.min(MAX_SPARSE_ENTRY_COUNT, slotCount / 2);
    }

    /**
     * Returns the logical index reserved for the given slot,
     * or the slot itself if it holds no reservation.
     * Runs on every draw, and therefore validates nothing.
     *
     * @param slot must be {@code >= 0} and {@code < slotCount}
     */
    public int resolve(int slot) {
        var dense = denseArray;
        if (dense != null) {
            var reservation = dense[slot];
            return reservation == 0 ? slot : reservation - 1;
        }
        // Gate on the array, not on sparseEntryCount; a reservation which was released leaves the log allocated.
        var log = sparseLog;
        if (log == null) {
            return slot;
        }
        var offset = findOffset(log, slot);
        return offset == NOT_FOUND ? slot : log[offset + 1];
    }

    /**
     * Makes the given slot resolve to the given logical index.
     * Replaces the reservation the slot already holds, if any.
     */
    public void reserve(int slot, int logicalIndex) {
        requireValidSlot(slot, "slot");
        requireValidSlot(logicalIndex, "logicalIndex");
        if (slot == logicalIndex) {
            release(slot); // The log holds only the slots that deviate from the identity default.
            return;
        }
        var dense = denseArray;
        if (dense == null) {
            var log = sparseLog;
            if (log != null) {
                // A retired slot stays in range, so the same slot can be reserved again;
                // a duplicate pair would make findOffset() return a stale logical index.
                var existingOffset = findOffset(log, slot);
                if (existingOffset != NOT_FOUND) {
                    log[existingOffset + 1] = logicalIndex;
                    return;
                }
            }
            if (sparseEntryCount < maxSparseEntryCount) {
                if (log == null) {
                    log = new int[maxSparseEntryCount << 1];
                    sparseLog = log;
                }
                var offset = sparseEntryCount << 1;
                log[offset] = slot;
                log[offset + 1] = logicalIndex;
                sparseEntryCount++;
                return;
            }
            dense = upgradeToDense();
        }
        dense[slot] = logicalIndex + 1;
    }

    /**
     * Makes the given slot resolve to itself again.
     * Does nothing if the slot holds no reservation.
     */
    public void release(int slot) {
        requireValidSlot(slot, "slot");
        var dense = denseArray;
        if (dense != null) {
            dense[slot] = 0;
            return;
        }
        var log = sparseLog;
        if (log == null) {
            return;
        }
        var offset = findOffset(log, slot);
        if (offset == NOT_FOUND) {
            return;
        }
        var lastOffset = (sparseEntryCount - 1) << 1; // The log order carries no meaning; move the last pair in.
        log[offset] = log[lastOffset];
        log[offset + 1] = log[lastOffset + 1];
        sparseEntryCount--;
    }

    private int[] upgradeToDense() {
        var dense = new int[slotCount];
        var log = sparseLog;
        if (log != null) { // Null when maxSparseEntryCount is zero, which reaches this method with no log at all.
            for (var entry = 0; entry < sparseEntryCount; entry++) {
                var offset = entry << 1;
                dense[log[offset]] = log[offset + 1] + 1;
            }
        }
        sparseLog = null; // Let the log be collected; it is never read again.
        sparseEntryCount = 0;
        denseArray = dense;
        return dense;
    }

    /**
     * @return the even offset of the pair for the given slot, or {@link #NOT_FOUND}
     */
    private int findOffset(int[] log, int slot) {
        var usedLength = sparseEntryCount << 1;
        for (var offset = 0; offset < usedLength; offset += 2) {
            if (log[offset] == slot) {
                return offset;
            }
        }
        return NOT_FOUND;
    }

    private void requireValidSlot(int slot, String name) {
        if (slot < 0 || slot >= slotCount) {
            throw slotOutOfRange(slot, name);
        }
    }

    /**
     * Builds {@link #requireValidSlot(int, String)}'s exception in a method of its own,
     * because {@code .formatted()} boxes its arguments into a varargs array
     * and calls {@link #toString()}, which together are most of the bytecode of the guard.
     * Inline, that keeps both this method and {@link #reserve(int, int)}, which calls it twice,
     * over the JIT's inlining threshold on a path walked once per retirement.
     */
    private IllegalArgumentException slotOutOfRange(int slot, String name) {
        return new IllegalArgumentException("The %s (%d) of map (%s) must be >= 0 and < slotCount (%d)."
                .formatted(name, slot, this, slotCount));
    }

    /**
     * Used for testing
     */
    boolean isDense() {
        return denseArray != null;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder("{");
        var renderedCount = 0;
        var dense = denseArray;
        if (dense != null) {
            for (var slot = 0; slot < dense.length; slot++) {
                var reservation = dense[slot];
                if (reservation == 0) {
                    continue;
                }
                if (renderedCount == MAX_TO_STRING_RESERVATION_COUNT) {
                    builder.append(", ...");
                    break;
                }
                appendReservation(builder, renderedCount, slot, reservation - 1);
                renderedCount++;
            }
        } else {
            var log = sparseLog;
            if (log != null) { // Never more than MAX_SPARSE_ENTRY_COUNT pairs, so this needs no limit.
                for (var entry = 0; entry < sparseEntryCount; entry++) {
                    var offset = entry << 1;
                    appendReservation(builder, renderedCount, log[offset], log[offset + 1]);
                    renderedCount++;
                }
            }
        }
        return builder.append('}').toString();
    }

    private static void appendReservation(StringBuilder builder, int renderedCount, int slot, int logicalIndex) {
        if (renderedCount > 0) {
            builder.append(", ");
        }
        builder.append(slot).append('=').append(logicalIndex);
    }

}

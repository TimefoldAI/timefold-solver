package ai.timefold.solver.core.impl.solver.termination;

import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A heavily specialized pair of
 * <a href="https://en.wikipedia.org/wiki/Circular_buffer">ring buffers</a>
 * that acts as a {@link NavigableMap} for getting the scores at a prior time.
 * <br/>
 * The ring buffers operate with the following assumptions:
 * <ul>
 * <li>Keys (i.e. {@link System#nanoTime()} are inserted in ascending order</li>
 * <li>Times are queried in ascending order</li>
 * <li>The queried time is probably near the start of the ring buffer</li>
 * </ul>
 * <br/>
 * {@link DiminishedReturnsScoreRingBuffer} automatically clear entries prior
 * to the queried time when polled (if the queried time is not in the
 * {@link DiminishedReturnsScoreRingBuffer}, then the largest entry prior
 * to the queried time is kept and returned).
 * The ring buffers will automatically resize when filled past their capacity.
 * 
 * @param <Score_> The score type
 */
final class DiminishedReturnsScoreRingBuffer<Score_ extends Score<Score_>> {
    /**
     * Arbitrary; 4096 is a common buffer size and is
     * the default page size in the linux kernel.
     */
    private static final int DEFAULT_CAPACITY = 4096;

    int readIndex;
    int writeIndex;
    private long[] nanoTimeRingBuffer;
    private InnerScore<Score_>[] scoreRingBuffer;

    DiminishedReturnsScoreRingBuffer() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    DiminishedReturnsScoreRingBuffer(int capacity) {
        this(0, 0, new long[capacity], new InnerScore[capacity]);
    }

    DiminishedReturnsScoreRingBuffer(int readIndex, int writeIndex,
            long[] nanoTimeRingBuffer, @Nullable InnerScore<Score_>[] scoreRingBuffer) {
        this.nanoTimeRingBuffer = nanoTimeRingBuffer;
        this.scoreRingBuffer = scoreRingBuffer;
        this.readIndex = readIndex;
        this.writeIndex = writeIndex;
    }

    record RingBufferState(int readIndex, int writeIndex,
            long[] nanoTimeRingBuffer, @Nullable InnerScore<?>[] scoreRingBuffer) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RingBufferState that)) {
                return false;
            }
            return readIndex == that.readIndex && writeIndex == that.writeIndex && Objects.deepEquals(nanoTimeRingBuffer,
                    that.nanoTimeRingBuffer) && Objects.deepEquals(scoreRingBuffer, that.scoreRingBuffer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(readIndex, writeIndex, Arrays.hashCode(nanoTimeRingBuffer), Arrays.hashCode(scoreRingBuffer));
        }

        @Override
        public String toString() {
            return "RingBufferState{" +
                    "readIndex=" + readIndex +
                    ", writeIndex=" + writeIndex +
                    ", nanoTimeRingBuffer=" + Arrays.toString(nanoTimeRingBuffer) +
                    ", scoreRingBuffer=" + Arrays.toString(scoreRingBuffer) +
                    '}';
        }
    }

    @NonNull
    RingBufferState getState() {
        return new RingBufferState(readIndex, writeIndex, nanoTimeRingBuffer, scoreRingBuffer);
    }

    @SuppressWarnings("unchecked")
    void resize() {
        var newCapacity = nanoTimeRingBuffer.length * 2;
        var newNanoTimeRingBuffer = new long[newCapacity];
        var newScoreRingBuffer = new InnerScore[newCapacity];

        if (readIndex < writeIndex) {
            // entries are [startIndex, writeIndex)
            var newLength = writeIndex - readIndex;
            System.arraycopy(nanoTimeRingBuffer, readIndex, newNanoTimeRingBuffer, 0, newLength);
            System.arraycopy(scoreRingBuffer, readIndex, newScoreRingBuffer, 0, newLength);
            readIndex = 0;
            writeIndex = newLength;
        } else {
            // entries are [readIndex, CAPACITY) + [0, writeIndex)
            var firstLength = nanoTimeRingBuffer.length - readIndex;
            var secondLength = writeIndex;
            var totalLength = firstLength + secondLength;

            System.arraycopy(nanoTimeRingBuffer, readIndex, newNanoTimeRingBuffer, 0, firstLength);
            System.arraycopy(scoreRingBuffer, readIndex, newScoreRingBuffer, 0, firstLength);
            System.arraycopy(nanoTimeRingBuffer, 0, newNanoTimeRingBuffer, firstLength, secondLength);
            System.arraycopy(scoreRingBuffer, 0, newScoreRingBuffer, firstLength, secondLength);
            readIndex = 0;
            writeIndex = totalLength;
        }
        nanoTimeRingBuffer = newNanoTimeRingBuffer;
        scoreRingBuffer = newScoreRingBuffer;
    }

    /**
     * Removes all elements in the buffer.
     */
    public void clear() {
        readIndex = 0;
        writeIndex = 0;
        Arrays.fill(nanoTimeRingBuffer, 0);
        Arrays.fill(scoreRingBuffer, null);
    }

    /**
     * Returns the first element of the score ring buffer.
     * Does not remove the element.
     * Raises an exception if the buffer is
     * empty.
     *
     * @return the first element of the score ring buffer
     */
    public @NonNull InnerScore<Score_> peekFirst() {
        var out = scoreRingBuffer[readIndex];
        if (out == null) {
            throw new IllegalStateException("Impossible state: buffer is empty");
        }
        return out;
    }

    /**
     * Adds a time-score pairing to the ring buffers, resizing the
     * ring buffers if necessary.
     *
     * @param nanoTime the {@link System#nanoTime()} when the score was produced.
     * @param score the score that was produced.
     */
    public void put(long nanoTime, @NonNull InnerScore<Score_> score) {
        if (nanoTimeRingBuffer[writeIndex] != 0L) {
            resize();
        }
        nanoTimeRingBuffer[writeIndex] = nanoTime;
        scoreRingBuffer[writeIndex] = score;
        writeIndex = (writeIndex + 1) % nanoTimeRingBuffer.length;
    }

    /**
     * Removes count elements from both ring buffers,
     * and returns the next element (which remains in the buffers).
     *
     * @param count the number of items to remove from both buffers.
     * @return the first element in the score ring buffer after the count
     *         elements were removed.
     */
    private @NonNull InnerScore<Score_> clearCountAndPeekNext(int count) {
        if (readIndex + count < nanoTimeRingBuffer.length) {
            Arrays.fill(nanoTimeRingBuffer, readIndex, readIndex + count, 0L);
            Arrays.fill(scoreRingBuffer, readIndex, readIndex + count, null);
            readIndex += count;
        } else {
            int remaining = count - (nanoTimeRingBuffer.length - readIndex);
            Arrays.fill(nanoTimeRingBuffer, readIndex, nanoTimeRingBuffer.length, 0L);
            Arrays.fill(scoreRingBuffer, readIndex, nanoTimeRingBuffer.length, null);
            Arrays.fill(nanoTimeRingBuffer, 0, remaining, 0L);
            Arrays.fill(scoreRingBuffer, 0, remaining, null);
            readIndex = remaining;
        }
        return scoreRingBuffer[readIndex];
    }

    /**
     * Returns the latest score prior or at the given time, and removes
     * all time-score pairings prior to it.
     *
     * @param nanoTime the queried time in nanoseconds.
     * @return the latest score prior to the given time.
     */
    public @NonNull InnerScore<Score_> pollLatestScoreBeforeTimeAndClearPrior(long nanoTime) {
        if (readIndex == writeIndex && nanoTimeRingBuffer[writeIndex] == 0L) {
            throw new IllegalStateException("Impossible state: buffer is empty");
        }

        // entries are [readIndex, CAPACITY) + [0, writeIndex)
        int end = (readIndex < writeIndex) ? writeIndex : nanoTimeRingBuffer.length;
        for (int i = readIndex; i < end; i++) {
            if (nanoTimeRingBuffer[i] == nanoTime) {
                return clearCountAndPeekNext(i - readIndex);
            }
            if (nanoTimeRingBuffer[i] > nanoTime) {
                return clearCountAndPeekNext(i - readIndex - 1);
            }
        }

        int countRead = end - readIndex;
        if (writeIndex < readIndex && writeIndex != 0) {
            if (nanoTimeRingBuffer[0] == nanoTime) {
                return clearCountAndPeekNext(countRead);
            }
            if (nanoTimeRingBuffer[0] > nanoTime) {
                return clearCountAndPeekNext(countRead - 1);
            }
            for (int i = 1; i < writeIndex; i++) {
                if (nanoTimeRingBuffer[i] == nanoTime) {
                    return clearCountAndPeekNext(countRead + i);
                }
                if (nanoTimeRingBuffer[i] > nanoTime) {
                    return clearCountAndPeekNext(countRead + i - 1);
                }
            }
            // Buffer has at least one element, and the query should always be
            // greater than it. Since we exited the loop, return the last element.
            return clearCountAndPeekNext(countRead + writeIndex - 1);
        } else {
            // Buffer has at least one element, and the query should always be
            // greater than it. Since we exited the loop, return the last element.
            return clearCountAndPeekNext(countRead - 1);
        }
    }
}

package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Circular buffer implementation for managing late scores,
 * enabling simpler reset logic.
 * When {@link #tryReset} is called,
 * instead of filling all slots,
 * an epoch counter is incremented,
 * allowing the action to avoid reloading the score array with the new score.
 */
final class LateAcceptanceScoreBuffer {

    // Late score fields
    private final InnerScore<?>[] scores;
    private int currentIndex = 0;
    private final int size;
    // All required epoch fields
    private final long[] slotEpoch;
    private long resetEpoch = 0;
    private InnerScore<?> resetScore = null;
    private boolean writtenSinceReset = false;

    LateAcceptanceScoreBuffer(int size, InnerScore<?> initialScore) {
        this.scores = new InnerScore[size];
        Arrays.fill(scores, initialScore);
        this.size = size;
        // By default,
        // the score is set to zero,
        // and it means all scores will be read initially.
        this.slotEpoch = new long[size];
    }

    <Score_ extends Score<Score_>> InnerScore<Score_> getCurrent() {
        return get(currentIndex);
    }

    @SuppressWarnings("unchecked")
    <Score_ extends Score<Score_>> InnerScore<Score_> get(int index) {
        if (slotEpoch[index] < resetEpoch) {
            return (InnerScore<Score_>) resetScore;
        }
        return (InnerScore<Score_>) scores[index];
    }

    /**
     * Update the score and advance the current late index.
     * 
     * @param score the score to be added to the buffer
     */
    void update(InnerScore<?> score) {
        scores[currentIndex] = score;
        slotEpoch[currentIndex] = resetEpoch;
        writtenSinceReset = true;
        currentIndex = (currentIndex + 1) % size;
    }

    /**
     * Lazily resets all slots to {@code newScore}.
     * Updating the score array is unnecessary since the related counter ensures the new score is returned if no changes have
     * occurred.
     *
     * @param newScore the score to be used to reset the buffer
     */
    void tryReset(InnerScore<?> newScore) {
        // Skips the reset action when no slot has been written since the last reset and the score is unchanged
        if (writtenSinceReset || !Objects.equals(newScore, resetScore)) {
            resetScore = newScore;
            resetEpoch++;
            writtenSinceReset = false;
        }
    }
}

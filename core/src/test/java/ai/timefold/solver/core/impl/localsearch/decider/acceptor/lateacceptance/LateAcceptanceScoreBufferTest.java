package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class LateAcceptanceScoreBufferTest {

    @Test
    void initialStateReturnsInitialScore() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        for (var i = 0; i < 3; i++) {
            assertThat(buffer.<SimpleScore> get(i)).isEqualTo(initialScore);
        }
        assertThat(buffer.<SimpleScore> getCurrent()).isEqualTo(initialScore);
    }

    @Test
    void updateWritesCurrentSlotAndAdvancesCursor() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        var newScore = InnerScore.fullyAssigned(SimpleScore.of(-500));
        buffer.update(newScore);
        assertThat(buffer.<SimpleScore> get(0)).isEqualTo(newScore);
        assertThat(buffer.<SimpleScore> get(1)).isEqualTo(initialScore);
        assertThat(buffer.<SimpleScore> get(2)).isEqualTo(initialScore);
        // getCurrent now points at slot 1
        assertThat(buffer.<SimpleScore> getCurrent()).isEqualTo(initialScore);
    }

    @Test
    void circularBufferWrapsAround() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        var score0 = InnerScore.fullyAssigned(SimpleScore.of(-100));
        var score1 = InnerScore.fullyAssigned(SimpleScore.of(-200));
        var score2 = InnerScore.fullyAssigned(SimpleScore.of(-300));
        buffer.update(score0);
        buffer.update(score1);
        buffer.update(score2);
        // After 3 updates the cursor wraps back to slot 0
        assertThat(buffer.<SimpleScore> getCurrent()).isEqualTo(score0);
        var score3 = InnerScore.fullyAssigned(SimpleScore.of(-400));
        buffer.update(score3);
        assertThat(buffer.<SimpleScore> get(0)).isEqualTo(score3);
        assertThat(buffer.<SimpleScore> getCurrent()).isEqualTo(score1);
    }

    @Test
    void tryResetLazilySetsAllSlots() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        buffer.update(InnerScore.fullyAssigned(SimpleScore.of(-500))); // writtenSinceReset = true
        var resetScore = InnerScore.fullyAssigned(SimpleScore.of(-200));
        buffer.tryReset(resetScore);
        for (var i = 0; i < 3; i++) {
            assertThat(buffer.<SimpleScore> get(i)).isEqualTo(resetScore);
        }
    }

    @Test
    void slotWrittenAfterResetRetainsOwnValue() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        buffer.update(initialScore); // write slot 0; cursor → 1
        var resetScore = InnerScore.fullyAssigned(SimpleScore.of(0));
        buffer.tryReset(resetScore); // epoch increments; all slots lazily return resetScore
        var postResetScore = InnerScore.fullyAssigned(SimpleScore.of(-50));
        buffer.update(postResetScore); // write slot 1 at new epoch; cursor → 2
        assertThat(buffer.<SimpleScore> get(0)).isEqualTo(resetScore); // written before reset → resetScore
        assertThat(buffer.<SimpleScore> get(1)).isEqualTo(postResetScore); // written after reset → own value
        assertThat(buffer.<SimpleScore> get(2)).isEqualTo(resetScore); // never written → resetScore
    }

    @Test
    void tryResetSkippedWhenNoWritesAndSameScore() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        var resetScore = InnerScore.fullyAssigned(SimpleScore.of(0));
        // First call always fires (resetScore starts as null)
        buffer.tryReset(resetScore);
        for (var i = 0; i < 3; i++) {
            assertThat(buffer.<SimpleScore> get(i)).isEqualTo(resetScore);
        }
        // No writes since reset, same score → skip; observable state is unchanged
        buffer.tryReset(resetScore);
        for (var i = 0; i < 3; i++) {
            assertThat(buffer.<SimpleScore> get(i)).isEqualTo(resetScore);
        }
    }

    @Test
    void tryResetFiresWhenWrittenSinceResetEvenWithSameScore() {
        var initialScore = InnerScore.fullyAssigned(SimpleScore.of(-1000));
        var buffer = new LateAcceptanceScoreBuffer(3, initialScore);
        var resetScore = InnerScore.fullyAssigned(SimpleScore.of(0));
        buffer.tryReset(resetScore); // epoch 0 → 1
        buffer.update(initialScore); // writtenSinceReset = true
        buffer.tryReset(resetScore); // same score but writtenSinceReset → epoch 1 → 2
        // Slot 0 was stamped at epoch 1, which is now < 2, so it falls back to resetScore
        for (var i = 0; i < 3; i++) {
            assertThat(buffer.<SimpleScore> get(i)).isEqualTo(resetScore);
        }
    }
}

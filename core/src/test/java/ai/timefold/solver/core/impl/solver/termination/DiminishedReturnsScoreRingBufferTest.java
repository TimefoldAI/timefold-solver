package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class DiminishedReturnsScoreRingBufferTest {

    @Test
    void testResizeBasic() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                1, 3,
                new long[] { 0, 2, 3, 0 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4))
                });
        buffer.resize();
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(2);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(2, 3, 0, 0, 0, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void testResizeWrapped() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                3, 1,
                new long[] { 3, 0, 0, 2 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2))
                });
        buffer.resize();
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(2);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(2, 3, 0, 0, 0, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void testPutEmpty() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 0,
                new long[] { 0, 0, 0, 0 },
                new InnerScore[] {
                        null,
                        null,
                        null,
                        null
                });
        buffer.put(1, InnerScore.fullyAssigned(SimpleScore.ONE));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(1);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                null,
                null,
                null);

        buffer.put(3, InnerScore.fullyAssigned(SimpleScore.of(2)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(2);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 3, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                null,
                null);

        buffer.put(5, InnerScore.fullyAssigned(SimpleScore.of(3)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 3, 5, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null);

        buffer.put(7, InnerScore.fullyAssigned(SimpleScore.of(4)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isZero();
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 3, 5, 7);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)));

        buffer.put(9, InnerScore.fullyAssigned(SimpleScore.of(5)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(5);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 3, 5, 7, 9, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                InnerScore.fullyAssigned(SimpleScore.of(5)),
                null,
                null,
                null);
    }

    @Test
    void testPutWrapped() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                2, 0,
                new long[] { 0, 0, 1, 3 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(-1)),
                        InnerScore.fullyAssigned(SimpleScore.ZERO),
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2))
                });
        buffer.put(5, InnerScore.fullyAssigned(SimpleScore.of(3)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(2);
        assertThat(bufferState.writeIndex()).isEqualTo(1);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(5, 0, 1, 3);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.ZERO),
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)));

        buffer.put(7, InnerScore.fullyAssigned(SimpleScore.of(4)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(2);
        assertThat(bufferState.writeIndex()).isEqualTo(2);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(5, 7, 1, 3);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)));

        buffer.put(9, InnerScore.fullyAssigned(SimpleScore.of(5)));
        bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(5);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(1, 3, 5, 7, 9, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                InnerScore.fullyAssigned(SimpleScore.of(5)),
                null,
                null,
                null);
    }

    @Test
    void testPeek() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                2, 0,
                new long[] { 0, 0, 1, 3 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(-1)),
                        InnerScore.fullyAssigned(SimpleScore.ZERO),
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2))
                });
        assertThat(buffer.peekFirst()).isEqualTo(InnerScore.fullyAssigned(SimpleScore.ONE));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(2);
        assertThat(bufferState.writeIndex()).isZero();
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 0, 1, 3);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(InnerScore.fullyAssigned(SimpleScore.of(-1)),
                InnerScore.fullyAssigned(SimpleScore.ZERO),
                InnerScore.fullyAssigned(SimpleScore.ONE),
                InnerScore.fullyAssigned(SimpleScore.of(2)));
    }

    @Test
    void testPeekEmpty() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 0,
                new long[] { 0, 0, 0, 0 },
                new InnerScore[] {
                        null,
                        null,
                        null,
                        null
                });
        assertThatCode(buffer::peekFirst).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("buffer is empty");
    }

    @Test
    void testClear() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                2, 0,
                new long[] { 0, 0, 1, 3 },
                new InnerScore[] {
                        null,
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2))
                });
        buffer.clear();
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isZero();
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 0, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(null,
                null,
                null,
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorFull() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 0,
                new long[] { 1, 2, 3, 4 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4))
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(2)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(2)));

        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(1);
        assertThat(bufferState.writeIndex()).isZero();
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 2, 3, 4);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(null,
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)));
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorEmpty() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 0,
                new long[] { 0, 0, 0, 0 },
                new InnerScore[] {
                        null,
                        null,
                        null,
                        null
                });
        assertThatCode(() -> buffer.pollLatestScoreBeforeTimeAndClearPrior(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorEqualNormal() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 3,
                new long[] { 1, 2, 3, 0 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        null
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(2)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(2)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(1);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 2, 3, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(null,
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorLessThanNormal() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 3,
                new long[] { 1, 2, 4, 0 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        null
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(3)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(2)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(1);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 2, 4, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(null,
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorOverflowNormal() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                0, 3,
                new long[] { 1, 2, 3, 0 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        null
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(10)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(3)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(2);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 0, 3, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(null,
                null,
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorEqualWrapped() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                4, 3,
                new long[] { 2, 3, 4, 0, 1 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE)
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(3)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(3)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(1);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 3, 4, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                null,
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                null,
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorLessThanWrapped() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                4, 3,
                new long[] { 2, 3, 5, 0, 1 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE)
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(4)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(3)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(1);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 3, 5, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                null,
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                null,
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorOverflowWrapped() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                4, 3,
                new long[] { 2, 3, 4, 0, 1 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE)
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(10)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(4)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(2);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 0, 4, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                null,
                null,
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                null,
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorEqualWrappedZero() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                4, 3,
                new long[] { 2, 3, 4, 0, 1 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE)
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(2)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(2)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isZero();
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(2, 3, 4, 0, 0);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                null,
                null);
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorLessThanWrappedZero() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                4, 3,
                new long[] { 3, 4, 5, 0, 1 },
                new InnerScore[] {
                        InnerScore.fullyAssigned(SimpleScore.of(2)),
                        InnerScore.fullyAssigned(SimpleScore.of(3)),
                        InnerScore.fullyAssigned(SimpleScore.of(4)),
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE)
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(2)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.ONE));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(4);
        assertThat(bufferState.writeIndex()).isEqualTo(3);
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(3, 4, 5, 0, 1);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                InnerScore.fullyAssigned(SimpleScore.of(2)),
                InnerScore.fullyAssigned(SimpleScore.of(3)),
                InnerScore.fullyAssigned(SimpleScore.of(4)),
                null,
                InnerScore.fullyAssigned(SimpleScore.ONE));
    }

    @Test
    void testPollLatestScoreBeforeTimeAndClearPriorOverflowWrappedZero() {
        var buffer = new DiminishedReturnsScoreRingBuffer<SimpleScore>(
                3, 0,
                new long[] { 0, 0, 0, 1, 2 },
                new InnerScore[] {
                        null,
                        null,
                        null,
                        InnerScore.fullyAssigned(SimpleScore.ONE),
                        InnerScore.fullyAssigned(SimpleScore.of(2))
                });
        assertThat(buffer.pollLatestScoreBeforeTimeAndClearPrior(10)).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(2)));
        var bufferState = buffer.getState();
        assertThat(bufferState.readIndex()).isEqualTo(4);
        assertThat(bufferState.writeIndex()).isZero();
        assertThat(bufferState.nanoTimeRingBuffer()).containsExactly(0, 0, 0, 0, 2);
        assertThat(bufferState.scoreRingBuffer()).containsExactly(
                null,
                null,
                null,
                null,
                InnerScore.fullyAssigned(SimpleScore.of(2)));
    }
}

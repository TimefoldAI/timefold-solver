package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

import org.junit.jupiter.api.Test;

class ConnectedRangeTrackerTest {
    private static class TestRange {
        int start;
        int end;

        public TestRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestRange range = (TestRange) o;
            return start == range.start && end == range.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public String toString() {
            return "(" + start + ", " + end + ")";
        }
    }

    private ConnectedRangeTracker<TestRange, Integer, Integer> getIntegerConnectedRangeTracker() {
        return new ConnectedRangeTracker<>(TestRange::getStart, TestRange::getEnd, (a, b) -> b - a);
    }

    @Test
    void testNonConsecutiveRanges() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        Range<TestRange, Integer> a = tree.getRange(new TestRange(0, 2));
        Range<TestRange, Integer> b = tree.getRange(new TestRange(3, 4));
        Range<TestRange, Integer> c = tree.getRange(new TestRange(5, 7));
        tree.add(a);
        tree.add(b);
        tree.add(c);

        var connectedRangeList =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRangeList).hasSize(3);

        assertThat(connectedRangeList.get(0)).containsExactly(new TestRange(0, 2));
        assertThat(connectedRangeList.get(0).hasOverlap()).isFalse();
        assertThat(connectedRangeList.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRangeList.get(0).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRangeList.get(1)).containsExactly(new TestRange(3, 4));
        assertThat(connectedRangeList.get(1).hasOverlap()).isFalse();
        assertThat(connectedRangeList.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRangeList.get(1).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRangeList.get(2)).containsExactly(new TestRange(5, 7));
        assertThat(connectedRangeList.get(2).hasOverlap()).isFalse();
        assertThat(connectedRangeList.get(2).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRangeList.get(2).getMaximumOverlap()).isEqualTo(1);

        verifyGaps(tree);
    }

    @Test
    void testConsecutiveRanges() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        Range<TestRange, Integer> a = tree.getRange(new TestRange(0, 2));
        Range<TestRange, Integer> b = tree.getRange(new TestRange(2, 4));
        Range<TestRange, Integer> c = tree.getRange(new TestRange(4, 7));
        tree.add(a);
        tree.add(b);
        tree.add(c);

        var connectedRangeList =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRangeList).hasSize(1);

        assertThat(connectedRangeList.get(0)).containsExactly(new TestRange(0, 2), new TestRange(2, 4), new TestRange(4, 7));
        assertThat(connectedRangeList.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRangeList.get(0).getMaximumOverlap()).isEqualTo(1);
        verifyGaps(tree);
    }

    @Test
    void testDuplicateRanges() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        Range<TestRange, Integer> a = tree.getRange(new TestRange(0, 2));
        Range<TestRange, Integer> b = tree.getRange(new TestRange(4, 7));
        tree.add(a);
        tree.add(a);
        tree.add(b);

        var connectedRangeList =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRangeList).hasSize(2);

        assertThat(connectedRangeList.get(0)).containsExactly(a.getValue(), a.getValue());
        assertThat(connectedRangeList.get(0).getMinimumOverlap()).isEqualTo(2);
        assertThat(connectedRangeList.get(0).getMaximumOverlap()).isEqualTo(2);
        assertThat(connectedRangeList.get(1)).containsExactly(b.getValue());
        assertThat(connectedRangeList.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRangeList.get(1).getMaximumOverlap()).isEqualTo(1);
        verifyGaps(tree);
    }

    @Test
    void testRangeRemoval() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        TestRange removedRange = new TestRange(2, 4);
        Range<TestRange, Integer> a = tree.getRange(new TestRange(0, 2));
        Range<TestRange, Integer> b = tree.getRange(removedRange);
        Range<TestRange, Integer> c = tree.getRange(new TestRange(4, 7));
        tree.add(a);
        tree.add(b);
        tree.add(c);

        // Imitate changing planning variables
        removedRange.setStart(10);
        removedRange.setEnd(12);

        tree.remove(b);

        var connectedRangeList =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRangeList).hasSize(2);

        assertThat(connectedRangeList.get(0)).containsExactly(new TestRange(0, 2));
        assertThat(connectedRangeList.get(1)).containsExactly(new TestRange(4, 7));
        verifyGaps(tree);
    }

    @Test
    void testRangeAddUpdatingOldGap() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        TestRange beforeAll = new TestRange(1, 2);
        TestRange newStart = new TestRange(3, 8);
        TestRange oldStart = new TestRange(4, 5);
        TestRange betweenOldAndNewStart = new TestRange(6, 7);
        TestRange afterAll = new TestRange(9, 10);

        tree.add(tree.getRange(beforeAll));
        verifyGaps(tree);

        tree.add(tree.getRange(afterAll));
        verifyGaps(tree);

        tree.add(tree.getRange(oldStart));
        verifyGaps(tree);

        tree.add(tree.getRange(betweenOldAndNewStart));
        verifyGaps(tree);

        tree.add(tree.getRange(newStart));
        verifyGaps(tree);
    }

    @Test
    void testOverlappingRange() {
        ConnectedRangeTracker<TestRange, Integer, Integer> tree = getIntegerConnectedRangeTracker();
        Range<TestRange, Integer> a = tree.getRange(new TestRange(0, 2));
        TestRange removedTestRange1 = new TestRange(1, 3);
        Range<TestRange, Integer> removedRange1 = tree.getRange(removedTestRange1);
        Range<TestRange, Integer> c = tree.getRange(new TestRange(2, 4));

        Range<TestRange, Integer> d = tree.getRange(new TestRange(5, 6));

        Range<TestRange, Integer> e = tree.getRange(new TestRange(7, 9));
        TestRange removedTestRange2 = new TestRange(7, 9);
        Range<TestRange, Integer> removedRange2 = tree.getRange(removedTestRange2);

        tree.add(a);
        tree.add(removedRange1);
        tree.add(c);
        tree.add(d);
        tree.add(e);
        tree.add(removedRange2);

        var connectedRanges =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRanges).hasSize(3);

        assertThat(connectedRanges.get(0)).containsExactly(a.getValue(), removedTestRange1, c.getValue());
        assertThat(connectedRanges.get(0).hasOverlap()).isTrue();
        assertThat(connectedRanges.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(0).getMaximumOverlap()).isEqualTo(2);

        assertThat(connectedRanges.get(1)).containsExactly(d.getValue());
        assertThat(connectedRanges.get(1).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(1).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(2)).containsExactly(e.getValue(), removedTestRange2);
        assertThat(connectedRanges.get(2).hasOverlap()).isTrue();
        assertThat(connectedRanges.get(2).getMinimumOverlap()).isEqualTo(2);
        assertThat(connectedRanges.get(2).getMaximumOverlap()).isEqualTo(2);

        verifyGaps(tree);

        // Simulate changing planning variables
        removedTestRange1.setStart(0);
        removedTestRange1.setEnd(10);

        tree.remove(removedRange1);

        connectedRanges = new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRanges).hasSize(3);

        assertThat(connectedRanges.get(0)).containsExactly(a.getValue(), c.getValue());
        assertThat(connectedRanges.get(0).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(0).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(1)).containsExactly(d.getValue());
        assertThat(connectedRanges.get(1).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(1).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(2)).containsExactly(e.getValue(), removedTestRange2);
        assertThat(connectedRanges.get(2).hasOverlap()).isTrue();
        assertThat(connectedRanges.get(2).getMinimumOverlap()).isEqualTo(2);
        assertThat(connectedRanges.get(2).getMaximumOverlap()).isEqualTo(2);

        verifyGaps(tree);

        // Simulate changing planning variables
        removedTestRange2.setStart(2);
        removedTestRange2.setEnd(4);

        tree.remove(removedRange2);
        connectedRanges = new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRanges).hasSize(3);

        assertThat(connectedRanges.get(0)).containsExactly(a.getValue(), c.getValue());
        assertThat(connectedRanges.get(0).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(0).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(1)).containsExactly(d.getValue());
        assertThat(connectedRanges.get(1).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(1).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(2)).containsExactly(e.getValue());
        assertThat(connectedRanges.get(2).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(2).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(2).getMaximumOverlap()).isEqualTo(1);

        verifyGaps(tree);
        Range<TestRange, Integer> g = tree.getRange(new TestRange(6, 7));
        tree.add(g);
        connectedRanges = new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        assertThat(connectedRanges).hasSize(2);

        assertThat(connectedRanges.get(0)).containsExactly(a.getValue(), c.getValue());
        assertThat(connectedRanges.get(0).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(0).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(0).getMaximumOverlap()).isEqualTo(1);

        assertThat(connectedRanges.get(1)).containsExactly(d.getValue(), g.getValue(), e.getValue());
        assertThat(connectedRanges.get(1).hasOverlap()).isFalse();
        assertThat(connectedRanges.get(1).getMinimumOverlap()).isEqualTo(1);
        assertThat(connectedRanges.get(1).getMaximumOverlap()).isEqualTo(1);
    }

    void verifyGaps(ConnectedRangeTracker<TestRange, Integer, Integer> tree) {
        var connectedRangeList =
                new IterableList<>(tree.getConnectedRangeChain().getConnectedRanges());
        var gapList =
                new IterableList<>(tree.getConnectedRangeChain().getGaps());

        if (connectedRangeList.size() == 0) {
            return;
        }
        assertThat(gapList).hasSize(connectedRangeList.size() - 1);
        for (int i = 0; i < connectedRangeList.size() - 1; i++) {
            assertThat(gapList.get(i).getPreviousRangeEnd()).isEqualTo(connectedRangeList.get(i).getEnd());
            assertThat(gapList.get(i).getNextRangeStart()).isEqualTo(connectedRangeList.get(i + 1).getStart());
            assertThat(gapList.get(i).getLength())
                    .isEqualTo(connectedRangeList.get(i + 1).getStart() - connectedRangeList.get(i).getEnd());
        }
    }

    private static int rangeGapCompare(RangeGap<Integer, Integer> a,
            RangeGap<Integer, Integer> b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return (a == null) ? -1 : 1;
        }
        boolean out = Objects.equals(a.getPreviousRangeEnd(), b.getPreviousRangeEnd()) &&
                Objects.equals(a.getNextRangeStart(), b.getNextRangeStart()) &&
                Objects.equals(a.getLength(), b.getLength());

        if (out) {
            return 0;
        }
        return a.hashCode() - b.hashCode();
    }

    private static int rangeClusterCompare(ConnectedRange<TestRange, Integer, Integer> a,
            ConnectedRange<TestRange, Integer, Integer> b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return (a == null) ? -1 : 1;
        }

        if (!(a instanceof ConnectedRangeImpl) || !(b instanceof ConnectedRangeImpl)) {
            throw new IllegalArgumentException("Expected (" + a + ") and (" + b + ") to both be ConnectedRangeImpl");
        }

        var first = (ConnectedRangeImpl<TestRange, Integer, Integer>) a;
        var second = (ConnectedRangeImpl<TestRange, Integer, Integer>) b;

        boolean out = first.getStartSplitPoint().compareTo(second.getStartSplitPoint()) == 0 &&
                first.getEndSplitPoint().compareTo(second.getEndSplitPoint()) == 0 &&
                first.getMinimumOverlap() == second.getMinimumOverlap() &&
                first.getMaximumOverlap() == second.getMaximumOverlap();
        if (out) {
            return 0;
        }
        return first.hashCode() - second.hashCode();
    }

    // Compare the mutable version with the recompute version
    @Test
    void testRandomRanges() {
        Random random = new Random(1);

        for (int i = 0; i < 100; i++) {
            Map<TestRange, Range<TestRange, Integer>> rangeToInstanceMap = new HashMap<>();
            TreeSet<RangeSplitPoint<TestRange, Integer>> splitPoints = new TreeSet<>();
            ConnectedRangeTracker<TestRange, Integer, Integer> tree =
                    new ConnectedRangeTracker<>(TestRange::getStart, TestRange::getEnd, (a, b) -> b - a);
            for (int j = 0; j < 100; j++) {
                // Create a random range
                String old = formatConnectedRangeTracker(tree);
                int from = random.nextInt(5);
                int to = from + random.nextInt(5);
                TestRange data = new TestRange(from, to);
                Range<TestRange, Integer> range = rangeToInstanceMap.computeIfAbsent(data, tree::getRange);
                Range<TestRange, Integer> treeRange =
                        new Range<>(data, TestRange::getStart, TestRange::getEnd);
                splitPoints.add(treeRange.getStartSplitPoint());
                splitPoints.add(treeRange.getEndSplitPoint());

                // Get the split points from the set (since those split points have collections)
                RangeSplitPoint<TestRange, Integer> startSplitPoint =
                        splitPoints.floor(treeRange.getStartSplitPoint());
                RangeSplitPoint<TestRange, Integer> endSplitPoint = splitPoints.floor(treeRange.getEndSplitPoint());

                // Create the collections if they do not exist
                if (startSplitPoint.startpointRangeToCountMap == null) {
                    startSplitPoint.createCollections();
                }
                if (endSplitPoint.endpointRangeToCountMap == null) {
                    endSplitPoint.createCollections();
                }

                // Either add or remove the range
                String op;
                if (startSplitPoint.containsRangeStarting(treeRange) && random.nextBoolean()) {
                    op = "Remove";
                    startSplitPoint.removeRangeStartingAtSplitPoint(treeRange);
                    endSplitPoint.removeRangeEndingAtSplitPoint(treeRange);
                    if (startSplitPoint.isEmpty()) {
                        splitPoints.remove(startSplitPoint);
                    }
                    if (endSplitPoint.isEmpty()) {
                        splitPoints.remove(endSplitPoint);
                    }
                    tree.remove(range);
                } else {
                    op = "Add";
                    startSplitPoint.addRangeStartingAtSplitPoint(treeRange);
                    endSplitPoint.addRangeEndingAtSplitPoint(treeRange);
                    tree.add(range);
                }

                // Recompute all connected ranges
                RangeSplitPoint<TestRange, Integer> previous = null;
                RangeSplitPoint<TestRange, Integer> current = splitPoints.isEmpty() ? null : splitPoints.first();
                List<ConnectedRangeImpl<TestRange, Integer, Integer>> rangeClusterList = new ArrayList<>();
                List<RangeGapImpl<TestRange, Integer, Integer>> gapList = new ArrayList<>();
                while (current != null) {
                    rangeClusterList
                            .add(ConnectedRangeImpl.getConnectedRangeStartingAt(splitPoints, (a, b) -> a - b, current));
                    if (previous != null) {
                        ConnectedRangeImpl<TestRange, Integer, Integer> before =
                                rangeClusterList.get(rangeClusterList.size() - 2);
                        ConnectedRangeImpl<TestRange, Integer, Integer> after =
                                rangeClusterList.get(rangeClusterList.size() - 1);
                        gapList.add(new RangeGapImpl<>(before, after, after.getStart() - before.getEnd()));
                    }
                    previous = current;
                    current = splitPoints.higher(rangeClusterList.get(rangeClusterList.size() - 1).getEndSplitPoint());
                }

                // Verify the mutable version matches the recompute version
                verifyGaps(tree);
                assertThat(tree.getConnectedRangeChain().getConnectedRanges())
                        .as(op + " range " + range + " to " + old)
                        .usingElementComparator(ConnectedRangeTrackerTest::rangeClusterCompare)
                        .containsExactlyElementsOf(rangeClusterList);
                assertThat(tree.getConnectedRangeChain().getGaps())
                        .as(op + " range " + range + " to " + old)
                        .usingElementComparator(ConnectedRangeTrackerTest::rangeGapCompare)
                        .containsExactlyElementsOf(gapList);
            }
        }
    }

    private String formatConnectedRangeTracker(ConnectedRangeTracker<TestRange, Integer, Integer> rangeTree) {
        List<List<TestRange>> listOfConnectedRanges = new ArrayList<>();
        for (ConnectedRange<TestRange, Integer, Integer> cluster : rangeTree.getConnectedRangeChain()
                .getConnectedRanges()) {
            List<TestRange> rangesInCluster = new ArrayList<>();
            for (TestRange range : cluster) {
                rangesInCluster.add(range);
            }
            listOfConnectedRanges.add(rangesInCluster);
        }
        return listOfConnectedRanges.stream()
                .map(cluster -> cluster.stream().map(TestRange::toString).collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(";", "{", "}"));
    }

}

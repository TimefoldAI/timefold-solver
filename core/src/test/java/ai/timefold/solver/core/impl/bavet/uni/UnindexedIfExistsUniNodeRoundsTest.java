package ai.timefold.solver.core.impl.bavet.uni;

import static ai.timefold.solver.core.impl.bavet.common.DeferredSettleAware.MIN_DEFER_DISTANCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.DeferredSettleAware;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Runs the same six rounds against both cross-match modes:
 * a settle distance below {@link DeferredSettleAware#MIN_DEFER_DISTANCE},
 * where the node reads the opposite side on the spot,
 * and one at it, where it defers the read to its own layer turn.
 * Both must agree with the brute-force answer in every round.
 * <p>
 * The first round loads both sides from empty; every round after it dirties only part of the node,
 * which is what makes the test able to catch a round that leaves bad state behind for the next.
 * <p>
 * It asserts the <i>answer</i>, not which path ran.
 * Unlike the join sibling, counting predicate evaluations cannot tell the two apart here:
 * a deferred right drain skips every pending left counter,
 * so {@code testFiltering} runs exactly left x right times either way.
 * <p>
 * {@link LiveTupleTracker} counts per tuple rather than collecting facts into a set,
 * so a doubled insert or an unbalanced retract fails here
 * instead of being absorbed into an answer that still looks right.
 */
class UnindexedIfExistsUniNodeRoundsTest {

    private static final BiPredicate<Integer, Integer> GREATER_THAN = (left, right) -> left > right;

    @ParameterizedTest
    @ValueSource(longs = { MIN_DEFER_DISTANCE - 1, MIN_DEFER_DISTANCE })
    void incrementalRoundsAgreeWithBruteForce(long settleDistance) {
        var fixture = new NodeFixture(settleDistance);
        var node = fixture.node;
        var leftTupleList = fixture.leftTupleList;
        var rightTupleList = fixture.rightTupleList;

        // Round one: filling from empty. Every left tuple is a fresh insert.
        for (var leftFact : List.of(0, 1, 2, 3, 4)) {
            fixture.insertLeft(leftFact);
        }
        for (var rightFact : List.of(2, 3)) {
            fixture.insertRight(rightFact);
        }
        fixture.settleAndAssert(); // Facts 3 and 4 exist.

        // Round two: one left tuple is dirtied by a retract/insert, and one right tuple arrives.
        // Four of the five counters stay clean, so the right drain has real work:
        // left fact 2 only starts existing because of the new right fact.
        var reinsertedLeftTuple = leftTupleList.getFirst();
        node.retractLeft(reinsertedLeftTuple);
        leftTupleList.remove(reinsertedLeftTuple);
        node.insertLeft(reinsertedLeftTuple);
        leftTupleList.add(reinsertedLeftTuple);
        fixture.insertRight(1);
        fixture.settleAndAssert();

        // Round three: updateRight. This is the path the skipped tracker-list clear depends on -
        // updateRight clears its own list before enqueueing.
        // Raising the only right fact below 2 takes left fact 2 back out.
        var updatedRightTuple = rightTupleList.get(2);
        updatedRightTuple.setA(9);
        node.updateRight(updatedRightTuple);
        fixture.settleAndAssert();

        // Round four: updateLeft, which reuses the existing counter rather than making a new one.
        var updatedLeftTuple = leftTupleList.get(1);
        updatedLeftTuple.setA(7);
        node.updateLeft(updatedLeftTuple);
        fixture.settleAndAssert();

        // Round five: retractRight, removing a fact that several left counters still match.
        var retractedRightTuple = rightTupleList.getFirst();
        node.retractRight(retractedRightTuple);
        rightTupleList.remove(retractedRightTuple);
        fixture.settleAndAssert();

        // Round six: drain the whole node. Every counter must end up retracted.
        for (var leftTuple : new ArrayList<>(leftTupleList)) {
            node.retractLeft(leftTuple);
            leftTupleList.remove(leftTuple);
        }
        fixture.settleAndAssert();
        assertThat(fixture.downstream.liveTupleMap).isEmpty();
    }

    /**
     * Everything a round mutates, or leaves stale enough to need a fresh instance:
     * the node, its propagator, the tuple lists the brute-force answer is computed from,
     * and the trackers the node is wired to.
     * Built per test rather than held in fields,
     * so no state can survive from one parameterized invocation into the next.
     */
    private static final class NodeFixture {

        private final LiveTupleTracker downstream = new LiveTupleTracker();
        private final SequentialPositionTracker positionTracker = new SequentialPositionTracker();
        private final UnindexedIfExistsUniNode<Integer, Integer> node =
                new UnindexedIfExistsUniNode<>(true, downstream, GREATER_THAN, positionTracker);
        private final Propagator propagator = node.getPropagator();
        private final List<UniTuple<Integer>> leftTupleList = new ArrayList<>();
        private final List<UniTuple<Integer>> rightTupleList = new ArrayList<>();

        private NodeFixture(long settleDistance) {
            node.setSettleDistance(settleDistance);
            node.afterAllFactsInsertedLeft(true);
            node.afterAllFactsInsertedRight(true);
        }

        private void insertLeft(int leftFact) {
            // Mimics an upstream node handing off a freshly inserted (and thus active) tuple,
            // matching UnindexedJoinBiNodeTest; UniTuple.of() leaves the state DEAD.
            var leftTuple = UniTuple.of(leftFact, positionTracker.leftSize);
            leftTuple.setState(TupleState.OK);
            leftTupleList.add(leftTuple);
            node.insertLeft(leftTuple);
        }

        private void insertRight(int rightFact) {
            var rightTuple = UniTuple.of(rightFact, positionTracker.rightSize);
            rightTuple.setState(TupleState.OK);
            rightTupleList.add(rightTuple);
            node.insertRight(rightTuple);
        }

        private void settleAndAssert() {
            node.prepareForSettle();
            propagator.propagateEverything();
            assertThat(downstream.liveFactSet()).containsExactlyInAnyOrderElementsOf(existingLeftFactSet());
        }

        /**
         * The brute-force answer the incremental node has to agree with.
         */
        private Set<Integer> existingLeftFactSet() {
            var existingFactSet = new LinkedHashSet<Integer>();
            for (var leftTuple : leftTupleList) {
                for (var rightTuple : rightTupleList) {
                    if (GREATER_THAN.test(leftTuple.getA(), rightTuple.getA())) {
                        existingFactSet.add(leftTuple.getA());
                        break;
                    }
                }
            }
            return existingFactSet;
        }

    }

    private static final class SequentialPositionTracker implements InTupleStorePositionTracker {

        private int leftSize = 0;
        private int rightSize = 0;

        @Override
        public int reserveNextLeft() {
            return leftSize++;
        }

        @Override
        public int reserveNextRight() {
            return rightSize++;
        }

    }

    /**
     * Tracks liveness per tuple instance, and fails on any lifecycle call that does not follow from the last one.
     * Counting rather than collecting is what makes a doubled insert or a surplus retract visible;
     * both are the shape of failure an inflated {@code countRight} or a duplicated tracker produces.
     */
    private static final class LiveTupleTracker implements TupleLifecycle<UniTuple<Integer>> {

        private final Map<UniTuple<Integer>, Integer> liveTupleMap = new IdentityHashMap<>();

        @Override
        public void insert(UniTuple<Integer> tuple) {
            int count = liveTupleMap.merge(tuple, 1, Integer::sum);
            if (count != 1) {
                throw new IllegalStateException(
                        "The tuple (%s) was inserted while its live count was already (%d)."
                                .formatted(tuple, count - 1));
            }
        }

        @Override
        public void update(UniTuple<Integer> tuple) {
            var count = liveTupleMap.getOrDefault(tuple, 0);
            if (count != 1) {
                throw new IllegalStateException(
                        "The tuple (%s) was updated while its live count was (%d).".formatted(tuple, count));
            }
        }

        @Override
        public void retract(UniTuple<Integer> tuple) {
            int count = liveTupleMap.merge(tuple, -1, Integer::sum);
            if (count != 0) {
                throw new IllegalStateException(
                        "The tuple (%s) was retracted while its live count was (%d).".formatted(tuple, count + 1));
            }
            liveTupleMap.remove(tuple);
        }

        @Override
        public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
            // Nothing to initialize; this lifecycle is always active.
        }

        @Override
        public boolean isActive() {
            return true;
        }

        private Set<Integer> liveFactSet() {
            var factSet = new LinkedHashSet<Integer>();
            for (var tuple : liveTupleMap.keySet()) {
                factSet.add(tuple.getA());
            }
            return factSet;
        }

    }

}

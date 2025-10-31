package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.UniqueRandomSequence;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An iterator for the bi-move stream which returns (A,B) pairs in random order.
 * It first needs to enumerate all tuples coming from the left, to be able to pick one at random.
 * For each left pick, it will enumerate all tuples coming from the right that match the joiner and filter,
 * to be able to pick one at random.
 * That selection is cached, effectively only happening the first time any particular left pick is made.
 * Once the selection is exhausted for a given left pick, that left pick is removed from the pool.
 * This means that this random move iterator will eventually end.
 */
@NullMarked
final class BiRandomMoveIterator<Solution_, A, B> implements Iterator<Move<Solution_>> {

    private final BiMoveStreamContext<Solution_, A, B> context;
    private final Random workingRandom;

    // Fields required for iteration.
    private final UniqueRandomSequence<UniTuple<A>> leftTupleSequence;
    private final Map<UniTuple<A>, UniqueRandomSequence<UniTuple<B>>> rightTupleSequenceMap;
    private @Nullable Move<Solution_> nextMove;

    public BiRandomMoveIterator(BiMoveStreamContext<Solution_, A, B> context, Random workingRandom) {
        this.context = Objects.requireNonNull(context);
        this.workingRandom = Objects.requireNonNull(workingRandom);
        var leftDatasetInstance = context.getLeftDatasetInstance();
        this.leftTupleSequence = leftDatasetInstance.buildRandomSequence(null);
        this.rightTupleSequenceMap = leftTupleSequence.isEmpty() ? Collections.emptyMap()
                : CollectionUtils.newIdentityHashMap(leftDatasetInstance.size());
    }

    private UniqueRandomSequence<UniTuple<B>> computeRightSequence(UniTuple<A> leftTuple) {
        var rightDatasetInstance = context.getRightDatasetInstance();
        var rightTupleCount = rightDatasetInstance.size();
        if (rightTupleCount == 0) {
            return UniqueRandomSequence.empty();
        }
        var leftFact = leftTuple.factA;
        var joiner = context.getJoiner();
        var filter = context.getFilter();
        var solutionView = context.neighborhoodSession().getSolutionView();
        var rightTupleSequence = rightDatasetInstance.buildRandomSequence(rightTuple -> {
            var rightFact = rightTuple.factA;
            if (failsJoiner(joiner, leftFact, rightFact)) {
                return false;
            }
            // Only test the filter after the joiners all match;
            // this fits user expectations as the filtering joiner is always declared last.
            return filter == null || filter.test(solutionView, leftFact, rightFact);
        });
        return rightTupleSequence.isEmpty() ? UniqueRandomSequence.empty() : rightTupleSequence;
    }

    static <A, B> boolean failsJoiner(DefaultBiEnumeratingJoiner<A, B> joiner, A leftFact, B rightFact) {
        var joinerCount = joiner.getJoinerCount();
        for (var joinerId = 0; joinerId < joinerCount; joinerId++) {
            var joinerType = joiner.getJoinerType(joinerId);
            var mappedLeft = joiner.getLeftMapping(joinerId).apply(leftFact);
            var mappedRight = joiner.getRightMapping(joinerId).apply(rightFact);
            if (!joinerType.matches(mappedLeft, mappedRight)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        if (nextMove != null) {
            return true;
        }

        while (!leftTupleSequence.isEmpty()) {
            var leftElement = leftTupleSequence.pick(workingRandom);
            var leftTuple = leftElement.value();
            var rightTupleSequence = rightTupleSequenceMap.computeIfAbsent(leftTuple, this::computeRightSequence);
            if (rightTupleSequence.isEmpty()) {
                leftTupleSequence.remove(leftElement.index());
                rightTupleSequenceMap.remove(leftTuple);
            } else {
                var bTuple = rightTupleSequence.remove(workingRandom);
                var leftFact = leftTuple.factA;
                var rightFact = bTuple.factA;
                nextMove = context.buildMove(leftFact, rightFact);
                return true;
            }
        }
        return false;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = Objects.requireNonNull(nextMove);
        nextMove = null;
        return result;
    }

}

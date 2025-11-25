package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingPredicate;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class JoiningIterator<Solution_, A, B> implements Iterator<UniTuple<B>> {

    private static final UniTuple EMPTY_TUPLE = new UniTuple<>(null, 0);

    private final SolutionView<Solution_> solutionView;
    private final DefaultBiEnumeratingJoiner<A, B> joiner;
    private final @Nullable BiEnumeratingPredicate<Solution_, A, B> filter;
    private final UniTuple<A> leftTuple;
    private final Iterator<UniTuple<B>> rightTupleIterator;

    // Required for iteration.
    private boolean hasNext = false;
    private UniTuple<B> next = EMPTY_TUPLE;

    public JoiningIterator(DefaultBiEnumeratingJoiner<A, B> joiner, @Nullable BiEnumeratingPredicate<Solution_, A, B> filter,
            SolutionView<Solution_> solutionView, UniTuple<A> leftTuple, Iterator<UniTuple<B>> rightTupleIterator) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.rightTupleIterator = Objects.requireNonNull(rightTupleIterator);
        this.joiner = Objects.requireNonNull(joiner);
        this.filter = filter;
        this.leftTuple = leftTuple;
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }

        var leftFact = leftTuple.factA;
        while (rightTupleIterator.hasNext()) {
            var rightTuple = rightTupleIterator.next();
            var rightFact = rightTuple.factA;
            if (failsJoiner(joiner, leftFact, rightFact)) {
                continue;
            }
            // Only test the filter after the joiners all match;
            // this fits user expectations as the filtering joiner is always declared last.
            if (filter == null || filter.test(solutionView, leftFact, rightFact)) {
                hasNext = true;
                next = rightTuple;
                return true;
            }
        }
        hasNext = false;
        next = EMPTY_TUPLE;
        return false;
    }

    private static <A, B> boolean failsJoiner(DefaultBiEnumeratingJoiner<A, B> joiner, A leftFact, B rightFact) {
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
    public UniTuple<B> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = Objects.requireNonNull(next);
        hasNext = false;
        next = EMPTY_TUPLE;
        return result;
    }
}

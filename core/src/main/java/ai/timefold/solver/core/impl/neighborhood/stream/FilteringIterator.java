package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class FilteringIterator<Solution_, A, B> implements Iterator<UniTuple<B>> {

    @SuppressWarnings("rawtypes")
    private static final UniTuple EMPTY_TUPLE = UniTuple.of(0);

    private final SolutionView<Solution_> solutionView;
    private final BiNeighborhoodsPredicate<Solution_, A, B> filter;
    private final UniTuple<A> leftTuple;
    private final Iterator<UniTuple<B>> rightTupleIterator;

    // Required for iteration.
    private boolean hasNext = false;

    @SuppressWarnings("unchecked")
    private UniTuple<B> next = EMPTY_TUPLE;

    public FilteringIterator(BiNeighborhoodsPredicate<Solution_, A, B> filter, SolutionView<Solution_> solutionView,
            UniTuple<A> leftTuple, Iterator<UniTuple<B>> rightTupleIterator) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.rightTupleIterator = Objects.requireNonNull(rightTupleIterator);
        this.filter = filter;
        this.leftTuple = leftTuple;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }

        var leftFact = leftTuple.getA();
        while (rightTupleIterator.hasNext()) {
            var rightTuple = rightTupleIterator.next();
            var rightFact = rightTuple.getA();
            if (filter.test(solutionView, leftFact, rightFact)) {
                hasNext = true;
                next = rightTuple;
                return true;
            }
        }
        hasNext = false;
        next = EMPTY_TUPLE;
        return false;
    }

    @SuppressWarnings("unchecked")
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

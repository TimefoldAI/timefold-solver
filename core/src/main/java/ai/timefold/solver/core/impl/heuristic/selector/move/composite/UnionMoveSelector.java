package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.move.UniformRandomUnionMoveIterator;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NonNull;

/**
 * A {@link CompositeMoveSelector} that unions 2 or more {@link MoveSelector}s.
 * <p>
 * For example: a union of {A, B, C} and {X, Y} will result in {A, B, C, X, Y}.
 * <p>
 * Warning: there is no duplicated {@link Move} check, so union of {A, B, C} and {B, D} will result in {A, B, C, B, D}.
 *
 * @see CompositeMoveSelector
 */
public final class UnionMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private final SelectionProbabilityWeightFactory<Solution_, MoveSelector<Solution_>> selectorProbabilityWeightFactory;

    private ScoreDirector<Solution_> scoreDirector;

    public UnionMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, boolean randomSelection) {
        this(childMoveSelectorList, randomSelection, null);
    }

    public UnionMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, boolean randomSelection,
            SelectionProbabilityWeightFactory<Solution_, MoveSelector<Solution_>> selectorProbabilityWeightFactory) {
        super(childMoveSelectorList, randomSelection);
        this.selectorProbabilityWeightFactory = selectorProbabilityWeightFactory;
        if (!randomSelection) {
            if (selectorProbabilityWeightFactory != null) {
                throw new IllegalArgumentException(
                        "The selector (%s) without randomSelection cannot have a selectorProbabilityWeightFactory (%s)."
                                .formatted(this, selectorProbabilityWeightFactory));
            }
        }
    }

    public SelectionProbabilityWeightFactory<Solution_, MoveSelector<Solution_>> getSelectorProbabilityWeightFactory() {
        return selectorProbabilityWeightFactory;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        scoreDirector = stepScope.getScoreDirector();
        super.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        scoreDirector = null;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        if (randomSelection) {
            for (var moveSelector : childMoveSelectorList) {
                if (moveSelector.isNeverEnding()) {
                    return true;
                }
            }
            // The UnionMoveSelector is special: it can be randomSelection true and still neverEnding false
            return false;
        } else {
            // Only the last childMoveSelector can be neverEnding
            return !childMoveSelectorList.isEmpty()
                    && childMoveSelectorList.getLast().isNeverEnding();
        }
    }

    @Override
    public long getSize() {
        var size = 0L;
        for (var moveSelector : childMoveSelectorList) {
            size += moveSelector.getSize();
        }
        return size;
    }

    @Override
    public @NonNull Iterator<Move<Solution_>> iterator() {
        if (!randomSelection) {
            var stream = Stream.<Move<Solution_>> empty();
            for (var moveSelector : childMoveSelectorList) {
                stream = Stream.concat(stream, toStream(moveSelector));
            }
            return stream.iterator();
        } else if (selectorProbabilityWeightFactory == null) {
            return UniformRandomUnionMoveIterator.of(workingRandom, childMoveSelectorList,
                    (moveSelector, workingRandom) -> moveSelector.iterator());
        } else {
            return new BiasedRandomUnionMoveIterator<>(childMoveSelectorList,
                    moveSelector -> {
                        var weight = selectorProbabilityWeightFactory.createProbabilityWeight(scoreDirector, moveSelector);
                        if (weight < 0.0) {
                            throw new IllegalStateException(
                                    "The selectorProbabilityWeightFactory (%s) returned a negative probabilityWeight (%f)."
                                            .formatted(selectorProbabilityWeightFactory, weight));
                        }
                        return weight;
                    }, workingRandom);
        }
    }

    private static <Solution_> Stream<Move<Solution_>> toStream(MoveSelector<Solution_> moveSelector) {
        return StreamSupport.stream(moveSelector.spliterator(), false);
    }

    @Override
    public String toString() {
        return "Union(%s)".formatted(childMoveSelectorList);
    }

}

package ai.timefold.solver.core.impl.heuristic.selector.common.iterator;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.move.Move;

public abstract class AbstractRandomSwapIterator<Solution_, Move_ extends Move<Solution_>, SubSelection_>
        implements Iterator<Move_> {

    protected final Iterable<SubSelection_> leftSubSelector;
    protected final Iterable<SubSelection_> rightSubSelector;

    protected Iterator<SubSelection_> leftSubSelectionIterator;
    protected Iterator<SubSelection_> rightSubSelectionIterator;

    private SubSelection_ leftSubSelection;

    public AbstractRandomSwapIterator(Iterable<SubSelection_> leftSubSelector,
            Iterable<SubSelection_> rightSubSelector) {
        this.leftSubSelector = leftSubSelector;
        this.rightSubSelector = rightSubSelector;
        leftSubSelectionIterator = this.leftSubSelector.iterator();
        rightSubSelectionIterator = this.rightSubSelector.iterator();
        // Don't do hasNext() in constructor (to avoid upcoming selections breaking mimic recording)
    }

    @Override
    public boolean hasNext() {
        leftSubSelection = null;
        if (!leftSubSelectionIterator.hasNext()) {
            leftSubSelectionIterator = leftSubSelector.iterator();
            if (!leftSubSelectionIterator.hasNext()) {
                return false;
            }
        }
        // The right selection may depend on selecting a left element first. E.g., FilteringEntityByEntitySelector
        leftSubSelection = leftSubSelectionIterator.next();
        if (!rightSubSelectionIterator.hasNext()) {
            rightSubSelectionIterator = rightSubSelector.iterator();
            if (!rightSubSelectionIterator.hasNext()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Move_ next() {
        if (leftSubSelection == null) {
            throw new IllegalStateException("Impossible state: no left element has been selected.");
        }
        // Ideally, this code should have read:
        //     SubS leftSubSelection = leftSubSelectionIterator.next();
        //     SubS rightSubSelection = rightSubSelectionIterator.next();
        // But empty selectors and ending selectors (such as non-random or shuffled) make it more complex
        SubSelection_ rightSubSelection = rightSubSelectionIterator.next();
        return newSwapSelection(leftSubSelection, rightSubSelection);
    }

    protected abstract Move_ newSwapSelection(SubSelection_ leftSubSelection, SubSelection_ rightSubSelection);

}

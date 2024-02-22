package ai.timefold.solver.core.impl.heuristic.selector.common.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

/**
 * IMPORTANT: The constructor of any subclass of this abstract class, should never call any of its child
 * {@link Selector}'s {@link Iterator#hasNext()} or {@link Iterator#next()} methods,
 * because that can cause descendant {@link Selector}s to be selected too early
 * (which breaks {@link MimicReplayingEntitySelector}).
 *
 * @param <S> Selection type, for example a {@link Move} class, an entity class or a value class.
 */
public abstract class UpcomingSelectionIterator<S> extends SelectionIterator<S> {

    protected boolean upcomingCreated = false;
    protected boolean hasUpcomingSelection = true;
    protected S upcomingSelection;

    @Override
    public boolean hasNext() {
        if (!upcomingCreated) {
            upcomingSelection = createUpcomingSelection();
            upcomingCreated = true;
        }
        return hasUpcomingSelection;
    }

    @Override
    public S next() {
        if (!hasUpcomingSelection) {
            throw new NoSuchElementException();
        }
        if (!upcomingCreated) {
            upcomingSelection = createUpcomingSelection();
        }
        upcomingCreated = false;
        return upcomingSelection;
    }

    protected abstract S createUpcomingSelection();

    protected S noUpcomingSelection() {
        hasUpcomingSelection = false;
        return null;
    }

    @Override
    public String toString() {
        if (!upcomingCreated) {
            return "Next upcoming (?)";
        } else if (!hasUpcomingSelection) {
            return "No next upcoming";
        } else {
            return "Next upcoming (" + upcomingSelection + ")";
        }
    }

    /**
     * Some destination iterators, such as nearby destination iterators, may return even elements which are pinned.
     * This is because the nearby matrix always picks from all nearby elements, and is unaware of any pinning.
     * This means that later we need to filter out the pinned elements, so that moves aren't generated for them.
     *
     * @param destinationIterator never null
     * @param listVariableDescriptor never null
     * @return null if no unpinned destination was found, at which point the iterator is exhausted.
     */
    public static ElementLocation findUnpinnedDestination(Iterator<ElementLocation> destinationIterator,
            ListVariableDescriptor<?> listVariableDescriptor) {
        while (destinationIterator.hasNext()) {
            var destination = destinationIterator.next();
            if (destination instanceof LocationInList locationInList) {
                var isPinned = listVariableDescriptor.isElementPinned(null, locationInList.entity(), locationInList.index());
                if (!isPinned) {
                    return destination;
                }
            } else { // Unassigned location can not be pinned.
                return destination;
            }
        }
        return null;
    }

}

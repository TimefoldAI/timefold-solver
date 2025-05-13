package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;

public class QueuedMultipleEntityValuePlacer<Solution_> extends AbstractEntityPlacer<Solution_>
        implements EntityPlacer<Solution_> {

    private final List<EntityPlacer<Solution_>> queuedPlacerList;
    private final boolean sequentialSelection;

    public QueuedMultipleEntityValuePlacer(EntityPlacerFactory<Solution_> factory,
            HeuristicConfigPolicy<Solution_> configPolicy, List<EntityPlacer<Solution_>> queuedPlacerList,
            boolean sequentialSelection) {
        super(factory, configPolicy);
        this.queuedPlacerList = queuedPlacerList;
        this.sequentialSelection = sequentialSelection;
        this.queuedPlacerList.forEach(queuedPlacer -> phaseLifecycleSupport.addEventListener(queuedPlacer));
    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        var filteredQueuedPlacerList = queuedPlacerList.stream()
                .map(placer -> placer.rebuildWithFilter(filter))
                .toList();
        return new QueuedMultipleEntityValuePlacer<>(factory, configPolicy, filteredQueuedPlacerList, sequentialSelection);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        if (sequentialSelection) {
            return new SequentialQueuedEntityValuePlacingIterator(queuedPlacerList);
        } else {
            return null;
        }
    }

    private class SequentialQueuedEntityValuePlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>> {

        private final List<EntityPlacer<Solution_>> queuedPlacerList;
        private EntityPlacer<Solution_> currentPlacer;
        private int index = 0;

        private SequentialQueuedEntityValuePlacingIterator(List<EntityPlacer<Solution_>> queuedPlacerList) {
            this.queuedPlacerList = queuedPlacerList;
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            if (pickNextPlacer()) {
                return currentPlacer.iterator().next();
            }
            return noUpcomingSelection();
        }

        private boolean pickNextPlacer() {
            if (currentPlacer != null && currentPlacer.iterator().hasNext()) {
                return true;
            }
            while (index < queuedPlacerList.size()) {
                currentPlacer = queuedPlacerList.get(index);
                if (!currentPlacer.iterator().hasNext()) {
                    index++;
                    continue;
                }
                return true;
            }
            return false;
        }
    }

}

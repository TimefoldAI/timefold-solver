package ai.timefold.solver.core.impl.constructionheuristic.placer;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

public interface EntityPlacer<Solution_> extends Iterable<Placement<Solution_>>, PhaseLifecycleListener<Solution_> {

    EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter);

}

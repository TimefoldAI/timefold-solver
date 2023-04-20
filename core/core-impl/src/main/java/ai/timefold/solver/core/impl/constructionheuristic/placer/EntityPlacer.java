package ai.timefold.solver.core.impl.constructionheuristic.placer;

import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

public interface EntityPlacer<Solution_> extends Iterable<Placement<Solution_>>, PhaseLifecycleListener<Solution_> {

}

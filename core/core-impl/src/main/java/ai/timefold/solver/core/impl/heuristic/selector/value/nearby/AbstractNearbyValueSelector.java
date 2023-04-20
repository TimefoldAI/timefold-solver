package ai.timefold.solver.core.impl.heuristic.selector.value.nearby;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.AbstractNearbySelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyRandom;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

abstract class AbstractNearbyValueSelector<Solution_, ChildSelector_ extends PhaseLifecycleListener<Solution_>, ReplayingSelector_ extends PhaseLifecycleListener<Solution_>>
        extends AbstractNearbySelector<Solution_, ChildSelector_, ReplayingSelector_>
        implements ValueSelector<Solution_> {

    protected AbstractNearbyValueSelector(ChildSelector_ childSelector, Object replayingSelector,
            NearbyDistanceMeter<?, ?> nearbyDistanceMeter, NearbyRandom nearbyRandom, boolean randomSelection) {
        super(childSelector, replayingSelector, nearbyDistanceMeter, nearbyRandom, randomSelection);
    }

}

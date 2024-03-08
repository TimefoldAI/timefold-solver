package ai.timefold.solver.quarkus.testdata.dummy;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public class DummyDistanceMeter implements NearbyDistanceMeter<Object, Object> {
    @Override
    public double getNearbyDistance(Object origin, Object destination) {
        return 0;
    }
}

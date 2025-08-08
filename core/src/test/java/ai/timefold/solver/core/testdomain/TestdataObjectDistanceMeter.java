package ai.timefold.solver.core.testdomain;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public class TestdataObjectDistanceMeter<T extends TestdataObject> implements NearbyDistanceMeter<T, TestdataObject> {

    @Override
    public double getNearbyDistance(T origin, TestdataObject destination) {
        return Math.abs(coordinate(destination) - coordinate(origin));
    }

    static int coordinate(TestdataObject o) {
        try {
            return Integer.parseInt(o.getCode());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

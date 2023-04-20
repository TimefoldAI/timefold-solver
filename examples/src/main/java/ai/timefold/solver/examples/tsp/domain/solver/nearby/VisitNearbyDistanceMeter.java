package ai.timefold.solver.examples.tsp.domain.solver.nearby;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.examples.tsp.domain.Standstill;
import ai.timefold.solver.examples.tsp.domain.Visit;

public class VisitNearbyDistanceMeter implements NearbyDistanceMeter<Visit, Standstill> {

    @Override
    public double getNearbyDistance(Visit origin, Standstill destination) {
        long distance = origin.getDistanceTo(destination);
        // If arriving early also inflicts a cost (more than just not using the vehicle more), such as the driver's wage, use this:
        //        if (origin instanceof TimeWindowedCustomer && destination instanceof TimeWindowedCustomer) {
        //            distance += ((TimeWindowedCustomer) origin).getTimeWindowGapTo((TimeWindowedCustomer) destination);
        //        }
        return distance;
    }

}

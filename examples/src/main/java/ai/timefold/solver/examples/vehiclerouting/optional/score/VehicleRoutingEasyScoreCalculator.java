package ai.timefold.solver.examples.vehiclerouting.optional.score;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.examples.vehiclerouting.domain.Customer;
import ai.timefold.solver.examples.vehiclerouting.domain.Vehicle;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedVehicleRoutingSolution;

public class VehicleRoutingEasyScoreCalculator
        implements EasyScoreCalculator<VehicleRoutingSolution, HardSoftLongScore> {

    @Override
    public HardSoftLongScore calculateScore(VehicleRoutingSolution solution) {
        boolean timeWindowed = solution instanceof TimeWindowedVehicleRoutingSolution;
        List<Customer> customerList = solution.getCustomerList();
        List<Vehicle> vehicleList = solution.getVehicleList();
        Map<Vehicle, Integer> vehicleDemandMap = new HashMap<>(vehicleList.size());
        for (Vehicle vehicle : vehicleList) {
            vehicleDemandMap.put(vehicle, 0);
        }
        long hardScore = 0L;
        long softScore = 0L;
        for (Customer customer : customerList) {
            Vehicle vehicle = customer.getVehicle();
            if (vehicle != null) {
                vehicleDemandMap.put(vehicle, vehicleDemandMap.get(vehicle) + customer.getDemand());
                // Score constraint distanceToPreviousStandstill
                softScore -= customer.getDistanceFromPreviousStandstill();
                if (customer.getNextCustomer() == null) {
                    // Score constraint distanceFromLastCustomerToDepot
                    softScore -= customer.getLocation().getDistanceTo(vehicle.getLocation());
                }
                if (timeWindowed) {
                    TimeWindowedCustomer timeWindowedCustomer = (TimeWindowedCustomer) customer;
                    long dueTime = timeWindowedCustomer.getDueTime();
                    Long arrivalTime = timeWindowedCustomer.getArrivalTime();
                    if (dueTime < arrivalTime) {
                        // Score constraint arrivalAfterDueTime
                        hardScore -= (arrivalTime - dueTime);
                    }
                }
            }
        }
        for (Map.Entry<Vehicle, Integer> entry : vehicleDemandMap.entrySet()) {
            int capacity = entry.getKey().getCapacity();
            int demand = entry.getValue();
            if (demand > capacity) {
                // Score constraint vehicleCapacity
                hardScore -= (demand - capacity);
            }
        }
        // Score constraint arrivalAfterDueTimeAtDepot is a built-in hard constraint in VehicleRoutingImporter
        return HardSoftLongScore.of(hardScore, softScore);
    }

}

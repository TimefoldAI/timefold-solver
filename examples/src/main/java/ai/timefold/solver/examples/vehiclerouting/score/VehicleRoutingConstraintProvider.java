package ai.timefold.solver.examples.vehiclerouting.score;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.sum;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.examples.vehiclerouting.domain.Customer;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;

public class VehicleRoutingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                vehicleCapacity(factory),
                distanceToPreviousStandstill(factory),
                distanceFromLastCustomerToDepot(factory),
                arrivalAfterMaxEndTime(factory)
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    protected Constraint vehicleCapacity(ConstraintFactory factory) {
        return factory.forEach(Customer.class)
                .filter(customer -> customer.getVehicle() != null)
                .groupBy(Customer::getVehicle, sum(Customer::getDemand))
                .filter((vehicle, demand) -> demand > vehicle.getCapacity())
                .penalizeLong(HardSoftLongScore.ONE_HARD,
                        (vehicle, demand) -> demand - vehicle.getCapacity())
                .asConstraint("vehicleCapacity");
    }

    // ************************************************************************
    // Soft constraints
    // ************************************************************************

    protected Constraint distanceToPreviousStandstill(ConstraintFactory factory) {
        return factory.forEach(Customer.class)
                .filter(customer -> customer.getVehicle() != null)
                .penalizeLong(HardSoftLongScore.ONE_SOFT,
                        Customer::getDistanceFromPreviousStandstill)
                .asConstraint("distanceToPreviousStandstill");
    }

    protected Constraint distanceFromLastCustomerToDepot(ConstraintFactory factory) {
        return factory.forEach(Customer.class)
                .filter(customer -> customer.getVehicle() != null && customer.getNextCustomer() == null)
                .penalizeLong(HardSoftLongScore.ONE_SOFT,
                        Customer::getDistanceToDepot)
                .asConstraint("distanceFromLastCustomerToDepot");
    }

    // ************************************************************************
    // TimeWindowed: additional hard constraints
    // ************************************************************************

    protected Constraint arrivalAfterMaxEndTime(ConstraintFactory factory) {
        return factory.forEach(TimeWindowedCustomer.class)
                .filter(customer -> customer.getVehicle() != null && customer.getArrivalTime() > customer.getMaxEndTime())
                .penalizeLong(HardSoftLongScore.ONE_HARD,
                        customer -> customer.getArrivalTime() - customer.getMaxEndTime())
                .asConstraint("arrivalAfterMaxEndTime");
    }

}

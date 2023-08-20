package ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.solver;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.examples.vehiclerouting.domain.Customer;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedDepot;

// TODO When this class is added only for TimeWindowedCustomer, use TimeWindowedCustomer instead of Customer
public class ArrivalTimeUpdatingVariableListener implements VariableListener<VehicleRoutingSolution, Customer> {

    @Override
    public void beforeEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer windowedCustomer) {
            updateArrivalTime(scoreDirector, windowedCustomer);
        }
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer windowedCustomer) {
            updateArrivalTime(scoreDirector, windowedCustomer);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer windowedCustomer) {
            updateArrivalTime(scoreDirector, windowedCustomer);
        }
    }

    protected void updateArrivalTime(ScoreDirector<VehicleRoutingSolution> scoreDirector,
            TimeWindowedCustomer sourceCustomer) {

        if (sourceCustomer.getVehicle() == null) {
            if (sourceCustomer.getArrivalTime() != null) {
                scoreDirector.beforeVariableChanged(sourceCustomer, "arrivalTime");
                sourceCustomer.setArrivalTime(null);
                scoreDirector.afterVariableChanged(sourceCustomer, "arrivalTime");
            }
            return;
        }

        Customer previousCustomer = sourceCustomer.getPreviousCustomer();
        Long departureTime;
        if (previousCustomer == null) {
            departureTime = ((TimeWindowedDepot) sourceCustomer.getVehicle().getDepot()).getReadyTime();
        } else {
            departureTime = ((TimeWindowedCustomer) previousCustomer).getDepartureTime();
        }
        TimeWindowedCustomer shadowCustomer = sourceCustomer;
        Long arrivalTime = calculateArrivalTime(shadowCustomer, departureTime);
        while (shadowCustomer != null && !Objects.equals(shadowCustomer.getArrivalTime(), arrivalTime)) {
            scoreDirector.beforeVariableChanged(shadowCustomer, "arrivalTime");
            shadowCustomer.setArrivalTime(arrivalTime);
            scoreDirector.afterVariableChanged(shadowCustomer, "arrivalTime");
            departureTime = shadowCustomer.getDepartureTime();
            shadowCustomer = (TimeWindowedCustomer) shadowCustomer.getNextCustomer();
            arrivalTime = calculateArrivalTime(shadowCustomer, departureTime);
        }
    }

    private Long calculateArrivalTime(TimeWindowedCustomer customer, Long previousDepartureTime) {
        if (customer == null || previousDepartureTime == null) {
            return null;
        }
        return previousDepartureTime + customer.getDistanceFromPreviousStandstill();
    }

}

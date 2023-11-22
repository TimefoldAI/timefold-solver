package ai.timefold.solver.examples.vehiclerouting.domain.timewindowed;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.examples.vehiclerouting.domain.Customer;
import ai.timefold.solver.examples.vehiclerouting.domain.location.Location;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.solver.ArrivalTimeUpdatingVariableListener;

@PlanningEntity
public class TimeWindowedCustomer extends Customer {

    // Times are multiplied by 1000 to avoid floating point arithmetic rounding errors
    private long minStartTime;
    private long maxEndTime;
    private long serviceDuration;

    // Shadow variable
    private Long arrivalTime;

    public TimeWindowedCustomer() {
    }

    public TimeWindowedCustomer(long id, Location location, int demand, long minStartTime, long maxEndTime, long serviceDuration) {
        super(id, location, demand);
        this.minStartTime = minStartTime;
        this.maxEndTime = maxEndTime;
        this.serviceDuration = serviceDuration;
    }

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public long getMinStartTime() {
        return minStartTime;
    }

    public void setMinStartTime(long minStartTime) {
        this.minStartTime = minStartTime;
    }

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public long getMaxEndTime() {
        return maxEndTime;
    }

    public void setMaxEndTime(long maxEndTime) {
        this.maxEndTime = maxEndTime;
    }

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public long getServiceDuration() {
        return serviceDuration;
    }

    public void setServiceDuration(long serviceDuration) {
        this.serviceDuration = serviceDuration;
    }

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    // Arguable, to adhere to API specs (although this works), nextCustomer should also be a source,
    // because this shadow must be triggered after nextCustomer (but there is no need to be triggered by nextCustomer)
    @ShadowVariable(variableListenerClass = ArrivalTimeUpdatingVariableListener.class, sourceVariableName = "vehicle")
    @ShadowVariable(variableListenerClass = ArrivalTimeUpdatingVariableListener.class, sourceVariableName = "previousCustomer")
    public Long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public Long getDepartureTime() {
        if (arrivalTime == null) {
            return null;
        }
        return Math.max(arrivalTime, minStartTime) + serviceDuration;
    }

    public boolean isArrivalBeforeMinStartTime() {
        return arrivalTime != null
                && arrivalTime < minStartTime;
    }

    public boolean isArrivalAfterMaxEndTime() {
        return arrivalTime != null
                && maxEndTime < arrivalTime;
    }

    /**
     * @return a positive number, the time multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public long getTimeWindowGapTo(TimeWindowedCustomer other) {
        // maxEndTime doesn't account for serviceDuration
        long latestDepartureTime = maxEndTime + serviceDuration;
        long otherLatestDepartureTime = other.getMaxEndTime() + other.getServiceDuration();
        if (latestDepartureTime < other.getMinStartTime()) {
            return other.getMinStartTime() - latestDepartureTime;
        }
        if (otherLatestDepartureTime < minStartTime) {
            return minStartTime - otherLatestDepartureTime;
        }
        return 0L;
    }

}

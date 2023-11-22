package ai.timefold.solver.examples.vehiclerouting.domain.timewindowed;

import ai.timefold.solver.examples.vehiclerouting.domain.Depot;
import ai.timefold.solver.examples.vehiclerouting.domain.location.Location;

public class TimeWindowedDepot extends Depot {

    // Times are multiplied by 1000 to avoid floating point arithmetic rounding errors
    private long minStartTime;
    private long maxEndTime;

    public TimeWindowedDepot() {
    }

    public TimeWindowedDepot(long id, Location location, long minStartTime, long maxEndTime) {
        super(id, location);
        this.minStartTime = minStartTime;
        this.maxEndTime = maxEndTime;
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

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}

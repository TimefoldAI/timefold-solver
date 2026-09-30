package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle watching a fact collection of visits that may be assigned to other vehicles,
 * which the list element block cannot represent, since its visits are not graph nodes.
 */
@PlanningEntity
public class TestdataMultiEntityChainWatchedVehicle extends TestdataObject {

    List<TestdataMultiEntityChainWatchedVisit> watchedVisits = new ArrayList<>();
    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainWatchedVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    @ShadowVariable(supplierName = "watchedEndTimeSupplier")
    Integer watchedEndTime;

    public TestdataMultiEntityChainWatchedVehicle() {
    }

    public TestdataMultiEntityChainWatchedVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        if (visits.isEmpty()) {
            return departureTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    @ShadowSources("watchedVisits[].endServiceTime")
    public Integer watchedEndTimeSupplier() {
        var max = departureTime;
        for (var watchedVisit : watchedVisits) {
            if (watchedVisit.getEndServiceTime() == null) {
                return null;
            }
            max = Math.max(max, watchedVisit.getEndServiceTime());
        }
        return max;
    }

    public List<TestdataMultiEntityChainWatchedVisit> getWatchedVisits() {
        return watchedVisits;
    }

    public void setWatchedVisits(List<TestdataMultiEntityChainWatchedVisit> watchedVisits) {
        this.watchedVisits = watchedVisits;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataMultiEntityChainWatchedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainWatchedVisit> visits) {
        this.visits = visits;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    public Integer getWatchedEndTime() {
        return watchedEndTime;
    }

    public void setWatchedEndTime(Integer watchedEndTime) {
        this.watchedEndTime = watchedEndTime;
    }
}

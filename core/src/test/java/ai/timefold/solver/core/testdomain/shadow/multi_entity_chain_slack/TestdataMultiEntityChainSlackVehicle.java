package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainSlackVehicle extends TestdataObject {

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainSlackVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataMultiEntityChainSlackVehicle() {
    }

    public TestdataMultiEntityChainSlackVehicle(String code) {
        super(code);
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        return visits.isEmpty() ? 0 : visits.getLast().getEndServiceTime();
    }

    public List<TestdataMultiEntityChainSlackVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainSlackVisit> visits) {
        this.visits = visits;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }
}

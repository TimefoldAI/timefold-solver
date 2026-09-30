package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A visit reading the end time of its own vehicle, which is sourced from the visits:
 * a block node standing for all of them would have to be computed both before and after that end time.
 */
@PlanningEntity
public class TestdataMultiEntityChainSlackVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainSlackVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainSlackVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    @ShadowVariable(supplierName = "slackSupplier")
    Integer slack;

    public TestdataMultiEntityChainSlackVisit() {
    }

    public TestdataMultiEntityChainSlackVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (previousVisit == null) {
            return duration;
        }
        var previousEndServiceTime = previousVisit.getEndServiceTime();
        return previousEndServiceTime == null ? null : previousEndServiceTime + duration;
    }

    @ShadowSources({ "vehicle", "vehicle.endTime", "endServiceTime" })
    public Integer slackSupplier() {
        if (vehicle == null || vehicle.getEndTime() == null || endServiceTime == null) {
            return null;
        }
        return vehicle.getEndTime() - endServiceTime;
    }

    public TestdataMultiEntityChainSlackVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainSlackVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainSlackVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainSlackVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public int getDuration() {
        return duration;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }

    public Integer getSlack() {
        return slack;
    }

    public void setSlack(Integer slack) {
        this.slack = slack;
    }
}

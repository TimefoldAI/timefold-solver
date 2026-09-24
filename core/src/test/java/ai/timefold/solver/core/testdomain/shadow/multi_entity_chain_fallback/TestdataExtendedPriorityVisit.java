package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

/**
 * A visit subclass declaring a declarative shadow variable the other visits of the same list
 * do not have, so an updater of the element class cannot be applied to every element.
 */
@PlanningEntity
public class TestdataExtendedPriorityVisit extends TestdataExtendedVisit {

    int deadline;

    @ShadowVariable(supplierName = "slackSupplier")
    Integer slack;

    public TestdataExtendedPriorityVisit() {
    }

    public TestdataExtendedPriorityVisit(String code, int duration, int deadline) {
        super(code, duration);
        this.deadline = deadline;
    }

    @ShadowSources("endServiceTime")
    public Integer slackSupplier() {
        if (endServiceTime == null) {
            return null;
        }
        return deadline - endServiceTime;
    }

    public int getDeadline() {
        return deadline;
    }

    public Integer getSlack() {
        return slack;
    }

    public void setSlack(Integer slack) {
        this.slack = slack;
    }

}

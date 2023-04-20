package ai.timefold.solver.spring.boot.autoconfigure.chained.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;

@PlanningEntity
public class TestdataChainedSpringEntity implements TestdataChainedSpringObject {

    @PlanningVariable(valueRangeProviderRefs = { "chainedAnchorRange",
            "chainedEntityRange" }, graphType = PlanningVariableGraphType.CHAINED)
    private TestdataChainedSpringObject previous;

    private TestdataChainedSpringEntity next;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public TestdataChainedSpringObject getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataChainedSpringObject previous) {
        this.previous = previous;
    }

    @Override
    public TestdataChainedSpringEntity getNext() {
        return next;
    }

    @Override
    public void setNext(TestdataChainedSpringEntity next) {
        this.next = next;
    }

}

package ai.timefold.solver.quarkus.testdata.chained.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;

@PlanningEntity
public class TestdataChainedQuarkusEntity implements TestdataChainedQuarkusObject {

    @PlanningVariable(valueRangeProviderRefs = { "chainedAnchorRange",
            "chainedEntityRange" }, graphType = PlanningVariableGraphType.CHAINED)
    private TestdataChainedQuarkusObject previous;

    private TestdataChainedQuarkusEntity next;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public TestdataChainedQuarkusObject getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataChainedQuarkusObject previous) {
        this.previous = previous;
    }

    @Override
    public TestdataChainedQuarkusEntity getNext() {
        return next;
    }

    @Override
    public void setNext(TestdataChainedQuarkusEntity next) {
        this.next = next;
    }

}

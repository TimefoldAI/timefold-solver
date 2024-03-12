package ai.timefold.solver.core.impl.testdata.domain.chained.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataChainedBrownEntity extends TestdataObject implements TestdataChainedMultiEntityChainElement {

    private TestdataChainedMultiEntityChainElement previousChainElement;

    public TestdataChainedBrownEntity() {
    }

    public TestdataChainedBrownEntity(String code) {
        super(code);
    }

    @PlanningVariable(
            valueRangeProviderRefs = { "brownRange", "anchorRange" },
            graphType = PlanningVariableGraphType.CHAINED)
    public TestdataChainedMultiEntityChainElement getPreviousChainElement() {
        return previousChainElement;
    }

    public void setPreviousChainElement(TestdataChainedMultiEntityChainElement previousChainElement) {
        this.previousChainElement = previousChainElement;
    }
}

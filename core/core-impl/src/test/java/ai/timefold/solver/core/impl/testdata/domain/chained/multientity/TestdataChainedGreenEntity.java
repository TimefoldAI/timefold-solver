package ai.timefold.solver.core.impl.testdata.domain.chained.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataChainedGreenEntity extends TestdataObject implements TestdataChainedMultiEntityChainElement {

    private TestdataChainedMultiEntityChainElement previousChainElement;

    public TestdataChainedGreenEntity() {
    }

    public TestdataChainedGreenEntity(String code) {
        super(code);
    }

    @PlanningVariable(
            valueRangeProviderRefs = { "greenRange", "anchorRange" },
            graphType = PlanningVariableGraphType.CHAINED)
    public TestdataChainedMultiEntityChainElement getPreviousChainElement() {
        return previousChainElement;
    }

    public void setPreviousChainElement(TestdataChainedMultiEntityChainElement previousChainElement) {
        this.previousChainElement = previousChainElement;
    }
}

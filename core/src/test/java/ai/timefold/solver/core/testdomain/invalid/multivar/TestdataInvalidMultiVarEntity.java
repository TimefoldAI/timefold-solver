package ai.timefold.solver.core.testdomain.invalid.multivar;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedObject;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;

@PlanningEntity
public class TestdataInvalidMultiVarEntity extends TestdataObject implements TestdataChainedObject {

    @PlanningVariable(valueRangeProviderRefs = { "chainedEntityRange", "chainedAnchorRange" },
            graphType = PlanningVariableGraphType.CHAINED)
    private TestdataChainedObject chainedValue;

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataMixedValue> valueList;

    public TestdataInvalidMultiVarEntity(String code) {
        super(code);
    }

    public TestdataChainedObject getChainedValue() {
        return chainedValue;
    }

    public void setChainedValue(TestdataChainedObject chainedValue) {
        this.chainedValue = chainedValue;
    }

    public List<TestdataMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedValue> valueList) {
        this.valueList = valueList;
    }
}

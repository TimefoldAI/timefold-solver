package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedMultiEntityFirstEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataMixedMultiEntityFirstValue> valueList;

    public TestdataMixedMultiEntityFirstEntity() {
        // Required for cloner
    }

    public TestdataMixedMultiEntityFirstEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public List<TestdataMixedMultiEntityFirstValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedMultiEntityFirstValue> valueList) {
        this.valueList = valueList;
    }
}

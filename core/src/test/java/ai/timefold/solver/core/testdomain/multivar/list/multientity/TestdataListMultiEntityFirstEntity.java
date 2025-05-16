package ai.timefold.solver.core.testdomain.multivar.list.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiEntityFirstEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataListMultiEntityFirstValue> valueList;

    public TestdataListMultiEntityFirstEntity() {
        // Required for cloner
    }

    public TestdataListMultiEntityFirstEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public List<TestdataListMultiEntityFirstValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListMultiEntityFirstValue> valueList) {
        this.valueList = valueList;
    }
}

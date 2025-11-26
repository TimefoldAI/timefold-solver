package ai.timefold.solver.core.testdomain.shadow.mixed;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataMixedValue> valueList;

    public TestdataMixedEntity() {
        valueList = new ArrayList<>();
    }

    public TestdataMixedEntity(String code) {
        super(code);
        valueList = new ArrayList<>();
    }

    public List<TestdataMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedValue> valueList) {
        this.valueList = valueList;
    }
}

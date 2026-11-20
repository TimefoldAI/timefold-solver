package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataListInheritanceBaseEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataValue> valueList = new ArrayList<>();

    public TestdataListInheritanceBaseEntity() {
    }

    public TestdataListInheritanceBaseEntity(String code, TestdataValue... values) {
        super(code);
        valueList.addAll(List.of(values));
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

}

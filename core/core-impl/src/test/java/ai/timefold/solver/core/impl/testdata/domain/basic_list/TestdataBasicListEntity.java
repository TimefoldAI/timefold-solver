package ai.timefold.solver.core.impl.testdata.domain.basic_list;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataBasicListEntity extends TestdataObject {

    public static TestdataBasicListEntity createWithValues(String code, TestdataValue value, TestdataValue... values) {
        // Set up shadow variables to preserve consistency.
        return new TestdataBasicListEntity(code, value, values);
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private TestdataValue value;

    @PlanningListVariable(valueRangeProviderRefs = "secondValueRange")
    private List<TestdataValue> valueList;

    public TestdataBasicListEntity() {
    }

    public TestdataBasicListEntity(String code, TestdataValue value, TestdataValue... valueList) {
        super(code);
        this.value = value;
        this.valueList = Arrays.asList(valueList);
    }

    public TestdataValue getValue() {
        return value;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }
}

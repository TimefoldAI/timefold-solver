package ai.timefold.solver.core.testdomain.mixed.singleentity.multipleprovider;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;

@PlanningEntity
public class TestdataMixedMultipleProviderEntity extends TestdataObject {

    @PlanningVariable
    private TestdataOtherValue basicValue;

    @PlanningListVariable
    private List<TestdataValue> valueList;

    private final List<TestdataValue> valueRange;

    public TestdataMixedMultipleProviderEntity() {
        // Required for cloner
        this.valueRange = new ArrayList<>();
    }

    public TestdataMixedMultipleProviderEntity(String code, List<TestdataValue> valueRange) {
        super(code);
        this.valueRange = valueRange;
        valueList = new ArrayList<>();
    }

    public TestdataOtherValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataOtherValue basicValue) {
        this.basicValue = basicValue;
    }

    @ValueRangeProvider
    public List<TestdataValue> getValueRange() {
        return valueRange;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }
}

package ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.method;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterSolution;

@PlanningEntity
public class TestdataInvalidMethodEntityProvidingWithParameterEntity extends TestdataObject {

    private List<TestdataValue> valueRange;

    private TestdataValue value;

    public TestdataInvalidMethodEntityProvidingWithParameterEntity() {
        // Required for cloning
    }

    public TestdataInvalidMethodEntityProvidingWithParameterEntity(String code, List<TestdataValue> valueRange) {
        this(code, valueRange, null);
    }

    public TestdataInvalidMethodEntityProvidingWithParameterEntity(String code, List<TestdataValue> valueRange,
            TestdataValue value) {
        super(code);
        this.valueRange = valueRange;
        this.value = value;
    }

    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    public void setValueRange(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }

    public List<TestdataValue> getValueRange1(TestdataEntityProvidingWithParameterSolution ignoredSolution) {
        return valueRange;
    }

    private void setValueRange1(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }

    List<TestdataValue> getValueRange2(TestdataEntityProvidingWithParameterSolution ignoredSolution) {
        return valueRange;
    }

    protected void setValueRange2(List<TestdataValue> valueRange) {
        this.valueRange = valueRange;
    }

    public List<TestdataValue> getValueRangeWithoutSetter() {
        return valueRange;
    }

}

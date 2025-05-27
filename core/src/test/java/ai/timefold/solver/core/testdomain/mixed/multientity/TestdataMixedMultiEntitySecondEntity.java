package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedMultiEntitySecondEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange",
            strengthComparatorClass = TestdataMixedMultiEntitySecondValueComparator.class)
    private TestdataMixedMultiEntitySecondValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataMixedMultiEntitySecondValue secondBasicValue;

    public TestdataMixedMultiEntitySecondEntity() {
        // Required for cloner
    }

    public TestdataMixedMultiEntitySecondEntity(String code) {
        super(code);
    }

    public TestdataMixedMultiEntitySecondValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataMixedMultiEntitySecondValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataMixedMultiEntitySecondValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataMixedMultiEntitySecondValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }
}

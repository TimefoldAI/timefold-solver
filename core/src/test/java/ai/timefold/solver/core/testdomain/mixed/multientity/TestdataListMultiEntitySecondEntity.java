package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiEntitySecondEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataListMultiEntitySecondValue basicValue;

    @PlanningVariable(valueRangeProviderRefs = "otherValueRange")
    private TestdataListMultiEntitySecondValue secondBasicValue;

    public TestdataListMultiEntitySecondEntity() {
        // Required for cloner
    }

    public TestdataListMultiEntitySecondEntity(String code) {
        super(code);
    }

    public TestdataListMultiEntitySecondValue getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(TestdataListMultiEntitySecondValue basicValue) {
        this.basicValue = basicValue;
    }

    public TestdataListMultiEntitySecondValue getSecondBasicValue() {
        return secondBasicValue;
    }

    public void setSecondBasicValue(TestdataListMultiEntitySecondValue secondBasicValue) {
        this.secondBasicValue = secondBasicValue;
    }
}

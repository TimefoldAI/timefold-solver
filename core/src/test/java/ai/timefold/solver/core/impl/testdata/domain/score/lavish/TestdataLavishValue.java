package ai.timefold.solver.core.impl.testdata.domain.score.lavish;

import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

public class TestdataLavishValue extends TestdataObject {

    private TestdataLavishValueGroup valueGroup;

    public TestdataLavishValue() {
    }

    public TestdataLavishValue(String code, TestdataLavishValueGroup valueGroup) {
        super(code);
        this.valueGroup = valueGroup;
    }

    public TestdataLavishValueGroup getValueGroup() {
        return valueGroup;
    }

    public void setValueGroup(TestdataLavishValueGroup valueGroup) {
        this.valueGroup = valueGroup;
    }

}

package ai.timefold.solver.test.api.score.stream.testdata;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public final class TestdataConstraintVerifierFirstEntity extends TestdataObject {

    private TestdataValue value;

    public TestdataConstraintVerifierFirstEntity(String code) {
        super(code);
    }

    public TestdataConstraintVerifierFirstEntity(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public String toString() {
        return "TestdataConstraintVerifierFirstEntity(" +
                "code='" + code + '\'' +
                ')';
    }
}

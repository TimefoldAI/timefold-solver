package ai.timefold.solver.core.testdomain.sort.invalid.twofactory;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.DummyValueFactory;
import ai.timefold.solver.core.testdomain.common.DummyWeightValueFactory;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataInvalidTwoFactorySortableEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", comparatorFactoryClass = DummyValueFactory.class,
            strengthWeightFactoryClass = DummyWeightValueFactory.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataInvalidTwoFactorySortableEntity() {
    }

    public TestdataInvalidTwoFactorySortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataSortableValue getValue() {
        return value;
    }

    public void setValue(TestdataSortableValue value) {
        this.value = value;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}

package ai.timefold.solver.core.testdomain.sort.invalid.twocomparator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.DummyValueComparator;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataInvalidTwoComparatorSortableEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", comparatorClass = DummyValueComparator.class,
            strengthComparatorClass = DummyValueComparator.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataInvalidTwoComparatorSortableEntity() {
    }

    public TestdataInvalidTwoComparatorSortableEntity(String code, int difficulty) {
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

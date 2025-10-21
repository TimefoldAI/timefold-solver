package ai.timefold.solver.core.testdomain.sort.invalid.mixed.comparator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.DummyValueComparator;
import ai.timefold.solver.core.testdomain.common.DummyValueFactory;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataInvalidMixedComparatorSortableEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", comparatorClass = DummyValueComparator.class,
            comparatorFactoryClass = DummyValueFactory.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataInvalidMixedComparatorSortableEntity() {
    }

    public TestdataInvalidMixedComparatorSortableEntity(String code, int difficulty) {
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

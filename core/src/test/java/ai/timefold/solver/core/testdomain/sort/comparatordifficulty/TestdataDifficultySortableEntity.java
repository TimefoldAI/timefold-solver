package ai.timefold.solver.core.testdomain.sort.comparatordifficulty;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableComparator;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(difficultyComparatorClass = TestSortableComparator.class)
public class TestdataDifficultySortableEntity extends TestdataObject implements TestSortableObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = TestSortableComparator.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataDifficultySortableEntity() {
    }

    public TestdataDifficultySortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataSortableValue getValue() {
        return value;
    }

    public void setValue(TestdataSortableValue value) {
        this.value = value;
    }

    @Override
    public int getComparatorValue() {
        return difficulty;
    }
}

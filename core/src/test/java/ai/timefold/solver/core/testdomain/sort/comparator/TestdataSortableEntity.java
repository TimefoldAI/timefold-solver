package ai.timefold.solver.core.testdomain.sort.comparator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = SortableEntityComparator.class)
public class TestdataSortableEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = SortableValueComparator.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataSortableEntity() {
    }

    public TestdataSortableEntity(String code, int difficulty) {
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

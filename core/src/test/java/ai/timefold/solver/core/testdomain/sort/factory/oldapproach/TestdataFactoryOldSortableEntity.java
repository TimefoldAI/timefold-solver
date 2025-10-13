package ai.timefold.solver.core.testdomain.sort.factory.oldapproach;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableFactory;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(difficultyWeightFactoryClass = TestSortableFactory.class)
public class TestdataFactoryOldSortableEntity extends TestdataObject implements TestSortableObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthWeightFactoryClass = TestSortableFactory.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataFactoryOldSortableEntity() {
    }

    public TestdataFactoryOldSortableEntity(String code, int difficulty) {
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

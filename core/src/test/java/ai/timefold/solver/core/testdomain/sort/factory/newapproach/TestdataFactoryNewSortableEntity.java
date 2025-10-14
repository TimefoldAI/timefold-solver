package ai.timefold.solver.core.testdomain.sort.factory.newapproach;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableFactory;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(comparatorFactoryClass = TestSortableFactory.class)
public class TestdataFactoryNewSortableEntity extends TestdataObject implements TestSortableObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", comparatorFactoryClass = TestSortableFactory.class)
    private TestdataSortableValue value;
    private int difficulty;

    public TestdataFactoryNewSortableEntity() {
    }

    public TestdataFactoryNewSortableEntity(String code, int difficulty) {
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

package ai.timefold.solver.core.testdomain.list.sort.factory;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestSortableFactory;
import ai.timefold.solver.core.testdomain.common.TestSortableObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity(difficultyWeightFactoryClass = TestSortableFactory.class)
public class TestdataListFactorySortableEntity extends TestdataObject implements TestSortableObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", comparatorFactoryClass = TestSortableFactory.class)
    private List<TestdataSortableValue> valueList;
    private int difficulty;

    public TestdataListFactorySortableEntity() {
    }

    public TestdataListFactorySortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSortableValue> valueList) {
        this.valueList = valueList;
    }

    @Override
    public int getComparatorValue() {
        return difficulty;
    }
}

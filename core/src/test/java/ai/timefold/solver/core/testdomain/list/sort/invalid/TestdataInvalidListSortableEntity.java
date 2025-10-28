package ai.timefold.solver.core.testdomain.list.sort.invalid;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.DummyValueComparator;
import ai.timefold.solver.core.testdomain.common.DummyValueFactory;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningEntity
public class TestdataInvalidListSortableEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", comparatorClass = DummyValueComparator.class,
            comparatorFactoryClass = DummyValueFactory.class)
    private List<TestdataSortableValue> valueList;
    private int difficulty;

    public TestdataInvalidListSortableEntity() {
    }

    public TestdataInvalidListSortableEntity(String code, int difficulty) {
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

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}

package ai.timefold.solver.core.testdomain.list.sort.comparator;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = ListSortableEntityComparator.class)
public class TestdataListSortableEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = ListSortableValueComparator.class)
    private List<TestdataListSortableValue> valueList;
    private int difficulty;

    public TestdataListSortableEntity() {
    }

    public TestdataListSortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataListSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListSortableValue> valueList) {
        this.valueList = valueList;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}

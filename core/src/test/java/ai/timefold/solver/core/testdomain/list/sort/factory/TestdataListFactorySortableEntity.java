package ai.timefold.solver.core.testdomain.list.sort.factory;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyWeightFactoryClass = ListSortableEntityFactory.class)
public class TestdataListFactorySortableEntity extends TestdataObject implements Comparable<TestdataListFactorySortableEntity> {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", strengthWeightFactoryClass = ListSortableValueFactory.class)
    private List<TestdataListFactorySortableValue> valueList;
    private int difficulty;

    public TestdataListFactorySortableEntity() {
    }

    public TestdataListFactorySortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataListFactorySortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListFactorySortableValue> valueList) {
        this.valueList = valueList;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(TestdataListFactorySortableEntity o) {
        return difficulty - o.difficulty;
    }
}

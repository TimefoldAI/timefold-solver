package ai.timefold.solver.core.testdomain.list.valuerange.compartor;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = ListSortableEntityComparator.class)
public class TestdataListSortableEntityProvidingEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", strengthComparatorClass = ListSortableValueComparator.class)
    private List<TestdataListSortableEntityProvidingValue> valueList;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataListSortableEntityProvidingValue> valueRange;

    private int difficulty;

    public TestdataListSortableEntityProvidingEntity() {
    }

    public TestdataListSortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataListSortableEntityProvidingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListSortableEntityProvidingValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListSortableEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataListSortableEntityProvidingValue> valueRange) {
        this.valueRange = valueRange;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}

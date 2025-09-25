package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyWeightFactoryClass = ListSortableEntityFactory.class)
public class TestdataListFactorySortableEntityProvidingEntity extends TestdataObject
        implements Comparable<TestdataListFactorySortableEntityProvidingEntity> {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange", strengthWeightFactoryClass = ListSortableValueFactory.class)
    private List<TestdataListFactorySortableEntityProvidingValue> valueList;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataListFactorySortableEntityProvidingValue> valueRange;

    private int difficulty;

    public TestdataListFactorySortableEntityProvidingEntity() {
    }

    public TestdataListFactorySortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
        this.valueList = new ArrayList<>();
    }

    public List<TestdataListFactorySortableEntityProvidingValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListFactorySortableEntityProvidingValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListFactorySortableEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataListFactorySortableEntityProvidingValue> valueRange) {
        this.valueRange = valueRange;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(TestdataListFactorySortableEntityProvidingEntity o) {
        return difficulty - o.difficulty;
    }
}

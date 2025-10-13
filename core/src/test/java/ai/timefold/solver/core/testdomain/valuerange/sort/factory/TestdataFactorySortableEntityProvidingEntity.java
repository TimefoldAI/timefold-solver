package ai.timefold.solver.core.testdomain.valuerange.sort.factory;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyWeightFactoryClass = SortableEntityFactory.class)
public class TestdataFactorySortableEntityProvidingEntity extends TestdataObject
        implements Comparable<TestdataFactorySortableEntityProvidingEntity> {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthWeightFactoryClass = SortableValueFactory.class)
    private TestdataFactorySortableEntityProvidingValue value;
    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataFactorySortableEntityProvidingValue> valueRange;

    private int difficulty;

    public TestdataFactorySortableEntityProvidingEntity() {
    }

    public TestdataFactorySortableEntityProvidingEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataFactorySortableEntityProvidingValue getValue() {
        return value;
    }

    public void setValue(TestdataFactorySortableEntityProvidingValue value) {
        this.value = value;
    }

    public List<TestdataFactorySortableEntityProvidingValue> getValueRange() {
        return valueRange;
    }

    public void setValueRange(List<TestdataFactorySortableEntityProvidingValue> valueRange) {
        this.valueRange = valueRange;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(TestdataFactorySortableEntityProvidingEntity o) {
        return difficulty - o.difficulty;
    }
}

package ai.timefold.solver.core.testdomain.sort.factory;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyWeightFactoryClass = SortableEntityFactory.class)
public class TestdataFactorySortableEntity extends TestdataObject implements Comparable<TestdataFactorySortableEntity> {

    @PlanningVariable(valueRangeProviderRefs = "valueRange", strengthWeightFactoryClass = SortableValueFactory.class)
    private TestdataFactorySortableValue value;
    private int difficulty;

    public TestdataFactorySortableEntity() {
    }

    public TestdataFactorySortableEntity(String code, int difficulty) {
        super(code);
        this.difficulty = difficulty;
    }

    public TestdataFactorySortableValue getValue() {
        return value;
    }

    public void setValue(TestdataFactorySortableValue value) {
        this.value = value;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(TestdataFactorySortableEntity o) {
        return difficulty - o.difficulty;
    }
}

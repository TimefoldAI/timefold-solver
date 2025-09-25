package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListFactorySortableValue extends TestdataObject implements Comparable<TestdataListFactorySortableValue> {

    private int strength;

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListFactorySortableEntity entity;

    public TestdataListFactorySortableValue() {
    }

    public TestdataListFactorySortableValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public TestdataListFactorySortableEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListFactorySortableEntity entity) {
        this.entity = entity;
    }

    @Override
    public int compareTo(TestdataListFactorySortableValue o) {
        return strength - o.strength;
    }
}

package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListFactorySortableEntityProvidingValue extends TestdataObject
        implements Comparable<TestdataListFactorySortableEntityProvidingValue> {

    private int strength;

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListFactorySortableEntityProvidingEntity entity;

    public TestdataListFactorySortableEntityProvidingValue() {
    }

    public TestdataListFactorySortableEntityProvidingValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public TestdataListFactorySortableEntityProvidingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListFactorySortableEntityProvidingEntity entity) {
        this.entity = entity;
    }

    @Override
    public int compareTo(TestdataListFactorySortableEntityProvidingValue o) {
        return strength - o.strength;
    }
}

package ai.timefold.solver.core.testdomain.list.valuerange.compartor;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = ListSortableValueComparator.class)
public class TestdataListSortableEntityProvidingValue extends TestdataObject {

    private int strength;

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListSortableEntityProvidingEntity entity;

    public TestdataListSortableEntityProvidingValue() {
    }

    public TestdataListSortableEntityProvidingValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public TestdataListSortableEntityProvidingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListSortableEntityProvidingEntity entity) {
        this.entity = entity;
    }
}

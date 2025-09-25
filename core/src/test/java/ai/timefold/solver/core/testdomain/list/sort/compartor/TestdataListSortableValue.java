package ai.timefold.solver.core.testdomain.list.sort.compartor;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = ListSortableValueComparator.class)
public class TestdataListSortableValue extends TestdataObject {

    private int strength;

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListSortableEntity entity;

    public TestdataListSortableValue() {
    }

    public TestdataListSortableValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public TestdataListSortableEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListSortableEntity entity) {
        this.entity = entity;
    }
}

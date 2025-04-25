package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.shadow;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public abstract class TestdataExtendedShadowShadowEntity {
    @ShadowVariable(
            variableListenerClass = TestdataExtendedShadowVariableListener.class,
            sourceVariableName = "myPlanningVariable",
            sourceEntityClass = TestdataExtendedShadowEntity.class)
    public List<TestdataExtendedShadowEntity> planningEntityList = new ArrayList<>();

    protected TestdataExtendedShadowShadowEntity() {
    }

    protected TestdataExtendedShadowShadowEntity(List<TestdataExtendedShadowEntity> planningEntityList) {
        this.planningEntityList = planningEntityList;
    }

}

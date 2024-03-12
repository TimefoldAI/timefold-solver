package ai.timefold.solver.core.impl.testdata.domain.extendedshadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataExtendedShadowEntity {

    public int desiredId;

    @PlanningVariable(allowsUnassigned = true)
    public TestdataExtendedShadowVariable myPlanningVariable;

    public TestdataExtendedShadowEntity() {
    }

    public TestdataExtendedShadowEntity(int desiredId) {
        this.desiredId = desiredId;
    }

}

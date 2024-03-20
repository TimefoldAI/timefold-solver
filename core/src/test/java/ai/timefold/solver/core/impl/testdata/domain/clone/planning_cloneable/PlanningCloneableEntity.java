package ai.timefold.solver.core.impl.testdata.domain.clone.planning_cloneable;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class PlanningCloneableEntity implements PlanningCloneable<PlanningCloneableEntity> {
    public String code;
    @PlanningVariable
    public TestdataValue value;

    public PlanningCloneableEntity(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public PlanningCloneableEntity createNewInstance() {
        return new PlanningCloneableEntity(code);
    }
}

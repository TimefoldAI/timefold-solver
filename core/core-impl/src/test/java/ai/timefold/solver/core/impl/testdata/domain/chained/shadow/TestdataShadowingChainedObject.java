package ai.timefold.solver.core.impl.testdata.domain.chained.shadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public interface TestdataShadowingChainedObject {

    /**
     * @return sometimes null
     */
    @InverseRelationShadowVariable(sourceVariableName = "chainedObject")
    TestdataShadowingChainedEntity getNextEntity();

    void setNextEntity(TestdataShadowingChainedEntity nextEntity);

}

package ai.timefold.solver.quarkus.testdomain.chained;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public interface TestdataChainedQuarkusObject {

    @InverseRelationShadowVariable(sourceVariableName = "previous")
    TestdataChainedQuarkusEntity getNext();

    void setNext(TestdataChainedQuarkusEntity next);

}

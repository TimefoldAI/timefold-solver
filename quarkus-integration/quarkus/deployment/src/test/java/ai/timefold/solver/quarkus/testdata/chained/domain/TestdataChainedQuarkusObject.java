package ai.timefold.solver.quarkus.testdata.chained.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public interface TestdataChainedQuarkusObject {

    @InverseRelationShadowVariable(sourceVariableName = "previous")
    TestdataChainedQuarkusEntity getNext();

    void setNext(TestdataChainedQuarkusEntity next);

}

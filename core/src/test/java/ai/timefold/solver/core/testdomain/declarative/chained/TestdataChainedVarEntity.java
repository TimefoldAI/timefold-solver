package ai.timefold.solver.core.testdomain.declarative.chained;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;

@PlanningEntity
public class TestdataChainedVarEntity extends TestdataChainedVarValue {
    String id;
    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED)
    TestdataChainedVarValue previous;

    public TestdataChainedVarEntity(String id, TestdataChainedVarValue previous) {
        this.id = id;
        this.previous = previous;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataChainedVarValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataChainedVarValue previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "TestdataBasicVarEntity{" +
                "id=" + id +
                ", previous=" + previous +
                '}';
    }
}

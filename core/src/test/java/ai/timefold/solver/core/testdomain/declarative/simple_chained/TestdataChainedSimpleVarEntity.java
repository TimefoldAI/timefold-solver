package ai.timefold.solver.core.testdomain.declarative.simple_chained;

import java.time.Duration;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;

@PlanningEntity
public class TestdataChainedSimpleVarEntity extends TestdataChainedSimpleVarValue {
    String id;

    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED)
    TestdataChainedSimpleVarValue previous;

    public TestdataChainedSimpleVarEntity() {
    }

    public TestdataChainedSimpleVarEntity(String id, Duration duration) {
        super(id, duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataChainedSimpleVarValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataChainedSimpleVarValue previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "TestdataChainedSimpleVarEntity{" +
                "id=" + id +
                ", previous=" + previous +
                '}';
    }
}

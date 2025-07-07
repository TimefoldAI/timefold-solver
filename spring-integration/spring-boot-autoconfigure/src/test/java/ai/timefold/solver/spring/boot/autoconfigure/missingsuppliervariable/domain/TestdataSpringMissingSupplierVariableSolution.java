package ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataSpringMissingSupplierVariableSolution {

    private List<String> valueList;
    private List<TestdataSpringMissingSupplierVariableEntity> entityList;

    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataSpringMissingSupplierVariableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataSpringMissingSupplierVariableEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}

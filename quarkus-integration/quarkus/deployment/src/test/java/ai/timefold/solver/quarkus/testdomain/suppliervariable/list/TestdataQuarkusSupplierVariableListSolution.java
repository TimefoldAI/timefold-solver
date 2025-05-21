package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataQuarkusSupplierVariableListSolution {

    private List<TestdataQuarkusSupplierVariableListValue> valueList;
    private List<TestdataQuarkusSupplierVariableListEntity> entityList;

    private SimpleScore score;

    @ValueRangeProvider
    @PlanningEntityCollectionProperty
    public List<TestdataQuarkusSupplierVariableListValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataQuarkusSupplierVariableListValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataQuarkusSupplierVariableListEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataQuarkusSupplierVariableListEntity> entityList) {
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

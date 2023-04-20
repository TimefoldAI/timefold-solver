package ai.timefold.solver.quarkus.it.reflection.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

@PlanningSolution
public class TestdataReflectionSolution {

    @ValueRangeProvider(id = "fieldValueRange")
    private List<String> fieldValueList;

    private List<String> methodValueList;

    @PlanningEntityCollectionProperty
    private List<TestdataReflectionEntity> entityList;

    @PlanningScore
    private HardSoftScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<String> getFieldValueList() {
        return fieldValueList;
    }

    public void setFieldValueList(List<String> fieldValueList) {
        this.fieldValueList = fieldValueList;
    }

    public List<String> getMethodValueList() {
        return methodValueList;
    }

    public void setMethodValueList(List<String> methodValueList) {
        this.methodValueList = methodValueList;
    }

    @ValueRangeProvider(id = "methodValueRange")
    public List<String> readMethodValueList() {
        return methodValueList;
    }

    public List<TestdataReflectionEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataReflectionEntity> entityList) {
        this.entityList = entityList;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}

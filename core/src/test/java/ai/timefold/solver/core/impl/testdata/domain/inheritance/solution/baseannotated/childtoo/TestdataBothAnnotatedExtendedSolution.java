package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataBothAnnotatedExtendedSolution extends TestdataBothAnnotatedSolution {

    public static TestdataBothAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataBothAnnotatedExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        var secondEntityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataBothAnnotatedChildEntity(idx++);
            var secondEntity = new TestdataBothAnnotatedChildEntity(idx++);
            entityList.add(entity);
            secondEntityList.add(secondEntity);
        }
        solution.setEntityList(entityList);
        solution.setSecondEntityList(secondEntityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @PlanningEntityCollectionProperty
    private List<TestdataBothAnnotatedChildEntity> secondEntityList;

    public List<TestdataBothAnnotatedChildEntity> getSecondEntityList() {
        return secondEntityList;
    }

    public void setSecondEntityList(List<TestdataBothAnnotatedChildEntity> secondEntityList) {
        this.secondEntityList = secondEntityList;
    }

    @Override
    public List<TestdataBothAnnotatedChildEntity> getEntityList() {
        return (List<TestdataBothAnnotatedChildEntity>) super.getEntityList();
    }
}

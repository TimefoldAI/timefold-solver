package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataBothAnnotatedAbstractExtendedSolution extends TestdataBothAnnotatedAbstractSolution {

    public static TestdataBothAnnotatedAbstractExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataBothAnnotatedAbstractExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataBothAnnotatedAbstractChildEntity>(entityListSize);
        var secondEntityList = new ArrayList<TestdataBothAnnotatedAbstractChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataBothAnnotatedAbstractChildEntity(idx++);
            var secondEntity = new TestdataBothAnnotatedAbstractChildEntity(idx++);
            entityList.add(entity);
            secondEntityList.add(secondEntity);
        }
        solution.setEntityList(entityList);
        solution.setSecondEntityList(secondEntityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @PlanningEntityCollectionProperty
    private List<TestdataBothAnnotatedAbstractChildEntity> secondEntityList;

    public List<TestdataBothAnnotatedAbstractChildEntity> getSecondEntityList() {
        return secondEntityList;
    }

    public void setSecondEntityList(List<TestdataBothAnnotatedAbstractChildEntity> secondEntityList) {
        this.secondEntityList = secondEntityList;
    }

    @Override
    public List<TestdataBothAnnotatedAbstractChildEntity> getEntityList() {
        return (List<TestdataBothAnnotatedAbstractChildEntity>) super.getEntityList();
    }
}

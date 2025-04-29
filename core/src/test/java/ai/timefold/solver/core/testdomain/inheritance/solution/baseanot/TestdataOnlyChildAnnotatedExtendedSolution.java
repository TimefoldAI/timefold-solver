package ai.timefold.solver.core.testdomain.inheritance.solution.baseanot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataOnlyChildAnnotatedExtendedSolution extends TestdataOnlyChildAnnotatedSolution {

    public static TestdataOnlyChildAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataOnlyChildAnnotatedExtendedSolution();
        var valueList = new ArrayList<String>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add("Generated Value " + i);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataOnlyChildAnnotatedChildEntity>(entityListSize);
        var secondEntityList = new ArrayList<TestdataOnlyChildAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataOnlyChildAnnotatedChildEntity(idx++);
            var secondEntity = new TestdataOnlyChildAnnotatedChildEntity(idx++);
            entityList.add(entity);
            secondEntityList.add(secondEntity);
        }
        solution.setEntityList(entityList);
        solution.setSecondEntityList(secondEntityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @PlanningEntityCollectionProperty
    private List<TestdataOnlyChildAnnotatedChildEntity> secondEntityList;

    public List<TestdataOnlyChildAnnotatedChildEntity> getSecondEntityList() {
        return secondEntityList;
    }

    public void setSecondEntityList(List<TestdataOnlyChildAnnotatedChildEntity> secondEntityList) {
        this.secondEntityList = secondEntityList;
    }

    @Override
    public List<TestdataOnlyChildAnnotatedChildEntity> getEntityList() {
        return (List<TestdataOnlyChildAnnotatedChildEntity>) super.getEntityList();
    }
}

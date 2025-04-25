package ai.timefold.solver.quarkus.testdata.inheritance.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedSolution;

@PlanningSolution
public class TestdataBothAnnotatedNoRawListExtendedSolution extends TestdataBothAnnotatedSolution {

    public static SolutionDescriptor<TestdataBothAnnotatedNoRawListExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataBothAnnotatedNoRawListExtendedSolution.class,
                TestdataBothAnnotatedChildEntity.class, TestdataEntity.class);
    }

    public static TestdataBothAnnotatedNoRawListExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataBothAnnotatedNoRawListExtendedSolution("s1");
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        var subValueList = new ArrayList<TestdataValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
            subValueList.add(new TestdataValue("Generated Subvalue " + i));
        }
        solution.setValueList(valueList);
        solution.setSubValueList(subValueList);
        var entityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        var secondEntityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        var objectEntityList = new ArrayList<>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            var secondEntity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            var thirdEntity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            entityList.add(entity);
            secondEntityList.add(secondEntity);
            objectEntityList.add(thirdEntity);
        }
        solution.setEntityList(entityList);
        solution.setSubEntityList(secondEntityList);
        solution.setObjectEntityList(objectEntityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    @PlanningEntityProperty
    private TestdataEntity entity;
    @PlanningEntityProperty
    private TestdataBothAnnotatedChildEntity subEntity;
    @PlanningEntityCollectionProperty
    private List<TestdataBothAnnotatedChildEntity> subEntityList;
    @PlanningEntityCollectionProperty
    private List<Object> objectEntityList;

    public TestdataBothAnnotatedNoRawListExtendedSolution() {
    }

    public TestdataBothAnnotatedNoRawListExtendedSolution(String code) {
        super(code);
    }

    public TestdataEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataEntity entity) {
        this.entity = entity;
    }

    public TestdataBothAnnotatedChildEntity getSubEntity() {
        return subEntity;
    }

    public void setSubEntity(TestdataBothAnnotatedChildEntity subEntity) {
        this.subEntity = subEntity;
    }

    public List<TestdataBothAnnotatedChildEntity> getSubEntityList() {
        return subEntityList;
    }

    public void setSubEntityList(List<TestdataBothAnnotatedChildEntity> subEntityList) {
        this.subEntityList = subEntityList;
    }

    public List<Object> getObjectEntityList() {
        return objectEntityList;
    }

    public void setObjectEntityList(List<Object> objectEntityList) {
        this.objectEntityList = objectEntityList;
    }

}

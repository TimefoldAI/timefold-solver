package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataBothAnnotatedExtendedSolution extends TestdataBothAnnotatedSolution {

    public static SolutionDescriptor<TestdataBothAnnotatedExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataBothAnnotatedExtendedSolution.class,
                TestdataBothAnnotatedChildEntity.class, TestdataEntity.class);
    }

    public static EntityDescriptor<TestdataBothAnnotatedExtendedSolution> buildEntityDescriptor() {
        return buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataEntity.class);
    }

    public static TestdataBothAnnotatedExtendedSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataBothAnnotatedExtendedSolution("s1");
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
        var rawEntityList = new ArrayList<TestdataBothAnnotatedChildEntity>(entityListSize);
        var idx = 0;
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            var secondEntity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            var thirdEntity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            var fourthEntity = new TestdataBothAnnotatedChildEntity(String.valueOf(idx++));
            entityList.add(entity);
            secondEntityList.add(secondEntity);
            objectEntityList.add(thirdEntity);
            rawEntityList.add(fourthEntity);
        }
        solution.setEntityList(entityList);
        solution.setSubEntityList(secondEntityList);
        solution.setObjectEntityList(objectEntityList);
        solution.setRawEntityList(rawEntityList);
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.<SimpleScore> of(Collections.EMPTY_MAP));
        return solution;
    }

    public static TestdataBothAnnotatedExtendedSolution generateSolution(int entityListSize, int subEntityListSize,
            int rawEntityListSize) {
        var solution = new TestdataBothAnnotatedExtendedSolution("solution");

        solution.setEntity(new TestdataEntity("entity-singleton"));
        solution.setSubEntity(new TestdataBothAnnotatedChildEntity("subEntity-singleton"));

        solution.setEntityList(IntStream.range(0, entityListSize)
                .mapToObj(i -> "entity" + i)
                .map(TestdataEntity::new)
                .toList());

        solution.setSubEntityList(IntStream.range(0, subEntityListSize)
                .mapToObj(i -> "subEntity" + i)
                .map(TestdataBothAnnotatedChildEntity::new)
                .toList());

        solution.setRawEntityList(IntStream.range(0, rawEntityListSize)
                .mapToObj(i -> "subEntity" + i + "-R")
                .map(TestdataBothAnnotatedChildEntity::new)
                .toList());

        solution.setObjectEntityList(Collections.emptyList());
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
    @PlanningEntityCollectionProperty
    private List rawEntityList;

    public TestdataBothAnnotatedExtendedSolution() {
    }

    public TestdataBothAnnotatedExtendedSolution(String code) {
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

    public List getRawEntityList() {
        return rawEntityList;
    }

    public void setRawEntityList(List rawEntityList) {
        this.rawEntityList = rawEntityList;
    }
}

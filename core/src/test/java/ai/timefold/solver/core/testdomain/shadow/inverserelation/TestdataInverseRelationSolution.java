package ai.timefold.solver.core.testdomain.shadow.inverserelation;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataInverseRelationSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataInverseRelationSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataInverseRelationSolution.class,
                TestdataInverseRelationEntity.class, TestdataInverseRelationValue.class);
    }

    public static TestdataInverseRelationSolution generateSolution(int valueListSize, int entityListSize) {
        var solution = new TestdataInverseRelationSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataInverseRelationValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            var value = new TestdataInverseRelationValue("Generated Value " + i);
            valueList.add(value);
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataInverseRelationEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var value = valueList.get(i % valueListSize);
            TestdataInverseRelationEntity entity = new TestdataInverseRelationEntity("Generated Entity " + i, value);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataInverseRelationValue> valueList;
    private List<TestdataInverseRelationEntity> entityList;

    private SimpleScore score;

    public TestdataInverseRelationSolution() {
    }

    public TestdataInverseRelationSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataInverseRelationValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataInverseRelationValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInverseRelationEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInverseRelationEntity> entityList) {
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

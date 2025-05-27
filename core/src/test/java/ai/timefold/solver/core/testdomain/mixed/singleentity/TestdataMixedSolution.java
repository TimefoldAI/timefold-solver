package ai.timefold.solver.core.testdomain.mixed.singleentity;

import static ai.timefold.solver.core.config.solver.PreviewFeature.DECLARATIVE_SHADOW_VARIABLES;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataMixedSolution {

    public static SolutionDescriptor<TestdataMixedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(Set.of(DECLARATIVE_SHADOW_VARIABLES), TestdataMixedSolution.class,
                TestdataMixedEntity.class, TestdataMixedValue.class, TestdataMixedOtherValue.class);
    }

    public static TestdataMixedSolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataMixedSolution();
        var valueList = new ArrayList<TestdataMixedValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataMixedOtherValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataMixedValue("Generated Value " + i));
        }
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataMixedOtherValue("Generated Other Value " + i, valueListSize - i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataMixedEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMixedEntity("Entity " + i, i);
            entityList.add(entity);
        }
        solution.setEntityList(entityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataMixedValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @PlanningEntityCollectionProperty
    private List<TestdataMixedOtherValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataMixedEntity> entityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataMixedValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMixedOtherValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataMixedOtherValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataMixedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMixedEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}

package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataListMultiEntitySolution {

    public static SolutionDescriptor<TestdataListMultiEntitySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataListMultiEntitySolution.class,
                TestdataListMultiEntityFirstEntity.class, TestdataListMultiEntitySecondEntity.class);
    }

    public static TestdataListMultiEntitySolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataListMultiEntitySolution();
        var valueList = new ArrayList<TestdataListMultiEntityFirstValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataListMultiEntitySecondValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataListMultiEntityFirstValue("Generated Value " + i));
        }
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataListMultiEntitySecondValue("Generated Other Value " + i));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataListMultiEntityFirstEntity>(entityListSize);
        var otherEntityList = new ArrayList<TestdataListMultiEntitySecondEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataListMultiEntityFirstEntity("Entity " + i);
            entityList.add(entity);
            var otherEntity = new TestdataListMultiEntitySecondEntity("Other Entity " + i);
            otherEntityList.add(otherEntity);
        }
        solution.setEntityList(entityList);
        solution.setOtherEntityList(otherEntityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataListMultiEntityFirstValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataListMultiEntitySecondValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataListMultiEntityFirstEntity> entityList;
    @PlanningEntityCollectionProperty
    private List<TestdataListMultiEntitySecondEntity> otherEntityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataListMultiEntityFirstValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataListMultiEntityFirstValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataListMultiEntitySecondValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataListMultiEntitySecondValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataListMultiEntityFirstEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataListMultiEntityFirstEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataListMultiEntitySecondEntity> getOtherEntityList() {
        return otherEntityList;
    }

    public void setOtherEntityList(List<TestdataListMultiEntitySecondEntity> otherEntityList) {
        this.otherEntityList = otherEntityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}

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
public class TestdataMixedMultiEntitySolution {

    public static SolutionDescriptor<TestdataMixedMultiEntitySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMixedMultiEntitySolution.class,
                TestdataMixedMultiEntityFirstEntity.class, TestdataMixedMultiEntitySecondEntity.class);
    }

    public static TestdataMixedMultiEntitySolution generateUninitializedSolution(int entityListSize, int valueListSize,
            int otherValueListSize) {
        var solution = new TestdataMixedMultiEntitySolution();
        var valueList = new ArrayList<TestdataMixedMultiEntityFirstValue>(valueListSize);
        var otherValueList = new ArrayList<TestdataMixedMultiEntitySecondValue>(otherValueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataMixedMultiEntityFirstValue("Generated Value " + i));
        }
        var strength = otherValueListSize * 10;
        for (int i = 0; i < otherValueListSize; i++) {
            otherValueList.add(new TestdataMixedMultiEntitySecondValue("Generated Other Value " + i, strength--));
        }
        solution.setValueList(valueList);
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataMixedMultiEntityFirstEntity>(entityListSize);
        var otherEntityList = new ArrayList<TestdataMixedMultiEntitySecondEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            var entity = new TestdataMixedMultiEntityFirstEntity("Entity " + i, i);
            entityList.add(entity);
            var otherEntity = new TestdataMixedMultiEntitySecondEntity("Other Entity " + i);
            otherEntityList.add(otherEntity);
        }
        solution.setEntityList(entityList);
        solution.setOtherEntityList(otherEntityList);
        return solution;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    private List<TestdataMixedMultiEntityFirstValue> valueList;
    @ValueRangeProvider(id = "otherValueRange")
    @ProblemFactCollectionProperty
    private List<TestdataMixedMultiEntitySecondValue> otherValueList;
    @PlanningEntityCollectionProperty
    private List<TestdataMixedMultiEntityFirstEntity> entityList;
    @PlanningEntityCollectionProperty
    private List<TestdataMixedMultiEntitySecondEntity> otherEntityList;
    @PlanningScore
    private SimpleScore score;

    public List<TestdataMixedMultiEntityFirstValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedMultiEntityFirstValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataMixedMultiEntitySecondValue> getOtherValueList() {
        return otherValueList;
    }

    public void setOtherValueList(List<TestdataMixedMultiEntitySecondValue> otherValueList) {
        this.otherValueList = otherValueList;
    }

    public List<TestdataMixedMultiEntityFirstEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataMixedMultiEntityFirstEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataMixedMultiEntitySecondEntity> getOtherEntityList() {
        return otherEntityList;
    }

    public void setOtherEntityList(List<TestdataMixedMultiEntitySecondEntity> otherEntityList) {
        this.otherEntityList = otherEntityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}

package ai.timefold.solver.core.testdomain.shadow.full;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataShadowedFullSolution extends TestdataObject {
    @PlanningEntityCollectionProperty
    List<TestdataShadowedFullEntity> entityList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataShadowedFullValue> valueList;

    @PlanningScore
    SimpleScore score;

    public static SolutionDescriptor<TestdataShadowedFullSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataShadowedFullSolution.class,
                TestdataShadowedFullEntity.class, TestdataShadowedFullValue.class);
    }

    public static TestdataShadowedFullSolution ofUninitializedSolution(int entityCount, int valueCount) {
        var entityList = new ArrayList<TestdataShadowedFullEntity>(entityCount);
        var valueList = new ArrayList<TestdataShadowedFullValue>(valueCount);

        for (int i = 0; i < entityCount; i++) {
            entityList.add(new TestdataShadowedFullEntity("e" + i));
        }

        for (int i = 0; i < valueCount; i++) {
            valueList.add(new TestdataShadowedFullValue("v" + i));
        }

        var out = new TestdataShadowedFullSolution();
        out.setEntityList(entityList);
        out.setValueList(valueList);

        return out;
    }

    public List<TestdataShadowedFullEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(
            List<TestdataShadowedFullEntity> entityList) {
        this.entityList = entityList;
    }

    public List<TestdataShadowedFullValue> getValueList() {
        return valueList;
    }

    public void setValueList(
            List<TestdataShadowedFullValue> valueList) {
        this.valueList = valueList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}

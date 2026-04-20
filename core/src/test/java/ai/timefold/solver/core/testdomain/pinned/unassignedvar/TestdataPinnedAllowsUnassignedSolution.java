package ai.timefold.solver.core.testdomain.pinned.unassignedvar;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataPinnedAllowsUnassignedSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataPinnedAllowsUnassignedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataPinnedAllowsUnassignedSolution.class,
                TestdataPinnedAllowsUnassignedEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataPinnedAllowsUnassignedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    public static TestdataPinnedAllowsUnassignedSolution generateSolution(int valueCount, int entityCount) {
        var solution = new TestdataPinnedAllowsUnassignedSolution("Generated Solution 0");
        var valueList = new ArrayList<TestdataValue>(valueCount);
        for (var i = 0; i < valueCount; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
        }
        var entityList = new ArrayList<TestdataPinnedAllowsUnassignedEntity>(entityCount);
        for (var i = 0; i < entityCount; i++) {
            var entity = new TestdataPinnedAllowsUnassignedEntity("Generated Entity " + i);
            entity.setValue(valueList.get(i % valueCount));
            entityList.add(entity);
        }
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataPinnedAllowsUnassignedEntity> entityList;

    private SimpleScore score;

    public TestdataPinnedAllowsUnassignedSolution() {
    }

    public TestdataPinnedAllowsUnassignedSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedAllowsUnassignedEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedAllowsUnassignedEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}

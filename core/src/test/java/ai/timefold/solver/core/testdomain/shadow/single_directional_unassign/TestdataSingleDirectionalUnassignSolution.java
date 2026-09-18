package ai.timefold.solver.core.testdomain.shadow.single_directional_unassign;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataSingleDirectionalUnassignSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataSingleDirectionalUnassignSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataSingleDirectionalUnassignSolution.class,
                TestdataSingleDirectionalUnassignEntity.class, TestdataSingleDirectionalUnassignValue.class);
    }

    public static PlanningSolutionMetaModel<TestdataSingleDirectionalUnassignSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataSingleDirectionalUnassignEntity> entities;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataSingleDirectionalUnassignValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataSingleDirectionalUnassignSolution() {
    }

    public TestdataSingleDirectionalUnassignSolution(String code, List<TestdataSingleDirectionalUnassignEntity> entities,
            List<TestdataSingleDirectionalUnassignValue> values) {
        super(code);
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataSingleDirectionalUnassignEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataSingleDirectionalUnassignEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataSingleDirectionalUnassignValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataSingleDirectionalUnassignValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}

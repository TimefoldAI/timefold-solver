package ai.timefold.solver.core.testdomain.shadow.shared_source;

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
public class TestdataSharedSourceSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataSharedSourceSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataSharedSourceSolution.class,
                TestdataSharedSourceEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataSharedSourceSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataSharedSourceEntity> entities;

    @ValueRangeProvider
    List<TestdataSharedSourceValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataSharedSourceSolution() {
    }

    public TestdataSharedSourceSolution(String code, List<TestdataSharedSourceEntity> entities,
            List<TestdataSharedSourceValue> values) {
        super(code);
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataSharedSourceEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataSharedSourceEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataSharedSourceValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataSharedSourceValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
